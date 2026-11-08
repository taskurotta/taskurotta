#!/usr/bin/env python3
"""E2E-симулятор taskurotta: N актёров, long-poll, start, решения — по протоколу реального
клиента TOP (POST /rest/tasks/{poll,start,release}, wire-формат проверен интроспекцией
Jackson сервера 12.5; описание протокола — в playbook.md плейбука e2e-regression-run).

Профили нагрузки:
  --profile peak  — поминутный профиль стартов из request.log инцидента (profile_peak.json,
                    110 мин, всего 7414), перед ним фон (warmup) и после — хвост (tail);
  --rate R        — ровный фон R стартов/мин на --duration-min (калибровка).
"""
import argparse
import json
import os
import random
import threading
import time
import uuid
import urllib.error
import urllib.request

# Число хопов процесса (рабочих задач после начальной). Взвешенное: среднее ~3.7,
# итого решений на процесс ~4.7 (инцидент: 34124 release / 7421 start = 4.6).
K_WEIGHTS = {1: 0.10, 2: 0.18, 3: 0.22, 4: 0.18, 5: 0.13, 6: 0.10, 7: 0.06, 8: 0.03}
K_LIST = [(k, w) for k, w in K_WEIGHTS.items()]


def str_arg(value):
    return {"dataType": "java.lang.String", "valueType": "PLAIN", "taskId": None,
            "ready": True, "promise": False, "jsonvalue": json.dumps(value)}


def task_container(process_id, actor_id, method, task_type):
    return {"taskId": str(uuid.uuid4()), "processId": process_id, "pass": None,
            "method": method, "actorId": actor_id, "type": task_type, "startTime": 0,
            "errorAttempts": 0, "args": [str_arg("e2e-payload")], "options": None,
            "unsafe": False, "failTypes": None}


class Sim:
    def __init__(self, args):
        self.args = args
        self.base = args.base_url.rstrip("/")
        self.out = args.out
        os.makedirs(self.out, exist_ok=True)
        self.lock = threading.Lock()
        self.stop = threading.Event()
        self.plans = {}  # processId -> {"actors": [..], "pos": int}
        self.c = {  # счётчики
            "polls200": 0, "polls204": 0, "poll_ms_sum": 0.0, "poll_ms_max": 0.0,
            "starts": 0, "releases": 0, "finished": 0, "err5xx": 0, "err_other": 0,
            "poll_timeouts": 0}
        self.err_samples = []

    def http(self, path, body, timeout=75):
        req = urllib.request.Request(self.base + path, data=json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json",
                                              "User-Agent": "e2e-sim/1.0"}, method="POST")
        t0 = time.time()
        for attempt in range(2):
            try:
                with urllib.request.urlopen(req, timeout=timeout) as r:
                    return r.status, r.read(), time.time() - t0
            except urllib.error.HTTPError as e:
                return e.code, e.read(), time.time() - t0
            except Exception:
                if attempt == 0:
                    continue
                return None, b"conn-error", time.time() - t0
        return None, b"conn-error", time.time() - t0

    # ---------------- актёр: long-poll цикл ----------------
    def actor_loop(self, idx):
        name = "e2e.a%d" % idx
        body = {"name": name, "version": "1.0"}
        while not self.stop.is_set():
            st, data, dt = self.http("/rest/tasks/poll", body)
            if st == 204 or (st is None and dt < 10):
                with self.lock:
                    self.c["polls204"] += 1
                continue
            if st == 200:
                tc = json.loads(data)
                with self.lock:
                    self.c["polls200"] += 1
                    self.c["poll_ms_sum"] += dt * 1000
                    self.c["poll_ms_max"] = max(self.c["poll_ms_max"], dt * 1000)
                self.decide(tc)
            else:
                self.on_error("poll", st, data)

    def decide(self, tc):
        pid = tc["processId"]
        with self.lock:
            plan = self.plans.get(pid)
            if plan is None:
                self.c["err_other"] += 1
                self.err_samples.append("no-plan %s" % pid)
                return
            pos = plan["pos"]
            plan["pos"] = pos + 1
            k = len(plan["actors"]) - 1
            if pos < k:
                nxt = plan["actors"][pos + 1]
                tasks = [task_container(pid, nxt, "work", "WORKER")]
            else:
                tasks = None
        think = min(self.args.thinking_max,
                    random.expovariate(1.0 / self.args.thinking_mean))
        if think > 0:
            time.sleep(think)
        if self.stop.is_set():
            return
        rel = {"taskId": tc["taskId"], "processId": tc["processId"], "pass": tc.get("pass"),
               "value": str_arg("e2e-result"), "errorContainer": None, "restartTime": -1,
               "tasks": tasks, "actorId": tc["actorId"], "executionTime": int(think * 1000)}
        st, data, dt = self.http("/rest/tasks/release", rel)
        with self.lock:
            if st == 200:
                self.c["releases"] += 1
                if pos >= k:
                    self.c["finished"] += 1
                    self.plans.pop(pid, None)
            else:
                self.c["err_other"] += 1
                self.err_samples.append("release %s %s" % (st, data[:200]))

    # ---------------- стартер процессов ----------------
    def start_process(self):
        k = random.choices([x[0] for x in K_LIST], [x[1] for x in K_LIST])[0]
        actors = ["e2e.a%d#1.0" % random.randrange(self.args.actors) for _ in range(k + 1)]
        pid = str(uuid.uuid4())
        body = task_container(pid, actors[0], "start", "DECIDER_START")
        # План регистрируем ДО POST /start: сервер ставит задачу в очередь в момент запроса,
        # long-poll актёра может забрать её раньше, чем ответ придёт сюда (гонка -> «no-plan»).
        with self.lock:
            self.plans[pid] = {"actors": actors, "pos": 0}
        st, data, dt = self.http("/rest/tasks/start", body)
        with self.lock:
            if st == 200:
                self.c["starts"] += 1
            else:
                self.plans.pop(pid, None)  # процесс не создан — план не нужен
                if st is not None and 500 <= st < 600:
                    self.c["err5xx"] += 1
                    self.err_samples.append("start 5xx %s" % data[:300])
                else:
                    self.c["err_other"] += 1
                    self.err_samples.append("start %s %s" % (st, data[:300]))

    def run_warmup_peak_tail(self):
        a = self.args
        prof = json.load(open(a.profile))["starts_per_minute"]
        warmup = int(a.warmup_min * a.bg_rate)
        for _ in range(warmup):
            self.start_process(); self.sle(60.0 / a.bg_rate)
        for minute, n in enumerate(prof):
            self.log_phase("peak", minute)
            self.burst(n)
        for _ in range(int(a.tail_min * a.bg_rate)):
            self.start_process(); self.sle(60.0 / a.bg_rate)
        self.log_phase("tail-done", 0)

    def burst(self, n):
        # n стартов в пределах ~60 сек: до 8 параллельных стартеров
        from concurrent.futures import ThreadPoolExecutor
        if n <= 1:
            self.start_process()
            return
        workers = min(8, n)
        share = -(-n // workers)  # ceil
        interval = 60.0 * workers / n  # каждый стартер делит минуту на свою долю
        def one(idx):
            for i in range(idx, n, workers):
                if self.stop.is_set():
                    return
                self.start_process()
                self.sle(interval)
        with ThreadPoolExecutor(max_workers=workers) as ex:
            list(ex.map(one, range(workers)))

    def run_flat(self):
        end = time.time() + self.args.duration_min * 60
        interval = 60.0 / self.args.rate
        while time.time() < end and not self.stop.is_set():
            self.start_process()
            self.sle(interval)

    def sle(self, secs):
        # сон с проверкой stop
        self.stop.wait(secs)

    def log_phase(self, phase, minute):
        with self.lock:
            print("[%s] phase=%s min=%d starts=%d finished=%d err5xx=%d err=%d" %
                  (time.strftime("%H:%M:%S"), phase, minute, self.c["starts"],
                   self.c["finished"], self.c["err5xx"], self.c["err_other"]), flush=True)

    def on_error(self, what, st, data):
        with self.lock:
            if st is None:
                self.c["poll_timeouts"] += 1
            elif st is not None and 500 <= st < 600:
                self.c["err5xx"] += 1
                self.err_samples.append("%s 5xx %s" % (what, data[:300]))
            else:
                self.c["err_other"] += 1
                self.err_samples.append("%s %s %s" % (what, st, data[:300]))

    # ---------------- основной цикл ----------------
    def run(self):
        a = self.args
        threads = [threading.Thread(target=self.actor_loop, args=(i,), daemon=True)
                   for i in range(a.actors)]
        for t in threads:
            t.start()
        t0 = time.time()
        if a.profile:
            self.run_warmup_peak_tail()
        else:
            self.run_flat()
        # дренаж: ждём завершения висящих процессов
        print("[%s] schedule done, draining up to %d min" %
              (time.strftime("%H:%M:%S"), a.drain_min), flush=True)
        self.stop.wait(a.drain_min * 60)
        self.stop.set()
        time.sleep(2)
        # финал
        with self.lock:
            live = len(self.plans)
            snap = dict(self.c)
            snap["poll_ms_avg"] = snap["poll_ms_sum"] / max(1, snap["polls200"])
            snap["live_processes"] = live
            snap["duration_sec"] = int(time.time() - t0)
            snap["err_samples"] = self.err_samples[:50]
        with open(os.path.join(self.out, "summary.json"), "w") as f:
            json.dump(snap, f, indent=1)
        print("SUMMARY:", json.dumps(snap, indent=1), flush=True)

    def counters_loop(self, interval=10):
        t0 = time.time()
        while not self.stop.is_set():
            time.sleep(interval)
            with self.lock:
                c = dict(self.c)
                c["poll_ms_avg"] = c["poll_ms_sum"] / max(1, c["polls200"])
                c["live_processes"] = len(self.plans)
                c["t"] = int(time.time() - t0)
            with open(os.path.join(self.out, "counters.jsonl"), "a") as f:
                f.write(json.dumps(c) + "\n")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--actors", type=int, default=363)
    p.add_argument("--base-url", default="http://localhost:8815")
    p.add_argument("--out", required=True)
    p.add_argument("--profile", default=None, help="path to profile_peak.json")
    p.add_argument("--warmup-min", type=float, default=10)
    p.add_argument("--tail-min", type=float, default=20)
    p.add_argument("--bg-rate", type=float, default=1.9, help="фон, стартов/мин")
    p.add_argument("--rate", type=float, default=2.0, help="калибровка: стартов/мин")
    p.add_argument("--duration-min", type=float, default=15, help="калибровка: длительность")
    p.add_argument("--thinking-mean", type=float, default=60.0,
                   help="среднее 'размышление' актёра, сек (экспонента)")
    p.add_argument("--thinking-max", type=float, default=600.0)
    p.add_argument("--drain-min", type=float, default=15)
    args = p.parse_args()
    sim = Sim(args)
    threading.Thread(target=sim.counters_loop, daemon=True).start()
    sim.run()


if __name__ == "__main__":
    main()
