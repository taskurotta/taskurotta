#!/usr/bin/env python3
"""Статистика request.log инцидента: поминутный разрез, статусы, латентность poll.
Старый инстанс = строки 1..OLD_END. Использование: logstats.py <путь к request.log>."""
import re
import sys
from collections import defaultdict

LOG = sys.argv[1] if len(sys.argv) > 1 else "request.log"
OLD_END = 1577190

pat = re.compile(
    r'^(?P<ip>\S+) -\s+-\s+\[(?P<ts>[^\]]+)\] "(?P<method>\w+) (?P<path>\S+) [^"]+" (?P<status>\d+).*? (?P<ms>\d+)$')

per_min = defaultdict(lambda: [0, 0, 0, 0])  # total, poll, release, start
status_counts = defaultdict(int)
poll_ms = []
start_ms = []
rel_ms = []
ips = set()
peak_window = []  # (minute, starts) 16:11-18:00 (окно пика в request.log инцидента)

with open(LOG, "r", errors="replace") as f:
    for i, line in enumerate(f, 1):
        if i > OLD_END:
            break
        m = pat.match(line)
        if not m:
            continue
        ts = m["ts"]  # dd/Mon/yyyy:HH:MM:SS +0000
        day = ts[0:6]
        hhmm = ts[12:17]
        path = m["path"]
        status = m["status"]
        ms = int(m["ms"])
        ips.add(m["ip"])
        bucket = per_min[(day, hhmm)]
        bucket[0] += 1
        status_counts[status] += 1
        if path == "/rest/tasks/poll":
            bucket[1] += 1
            if len(poll_ms) < 400000:
                poll_ms.append(ms)
        elif path == "/rest/tasks/release":
            bucket[2] += 1
            if len(rel_ms) < 400000:
                rel_ms.append(ms)
        elif path == "/rest/tasks/start":
            bucket[3] += 1
            if len(start_ms) < 400000:
                start_ms.append(ms)
            if "16:11" <= hhmm <= "18:00":
                peak_window.append((hhmm, ms))

print("== unique IPs (old):", len(ips))
print("== status counts:", dict(sorted(status_counts.items())))


def q(lst, p):
    if not lst:
        return 0
    lst = sorted(lst)
    return lst[int(len(lst) * p)]


def stats(name, lst):
    print(f"== {name}: n={len(lst)} p50={q(lst,0.5)} p90={q(lst,0.9)} p99={q(lst,0.99)} max={max(lst) if lst else 0}")


stats("poll ms", poll_ms)
stats("start ms", start_ms)
stats("release ms", rel_ms)

print("== peak window 16:11-18:00 (по минутам):")
mins = sorted(set(x[0] for x in peak_window))
print("minutes:", len(mins))
starts_per_min = defaultdict(int)
for m_, _ in peak_window:
    starts_per_min[m_] += 1
vals = [starts_per_min[m_] for m_ in mins]
print(f"start/min: min={min(vals)} max={max(vals)} avg={sum(vals)/len(vals):.1f} total={sum(vals)}")

print("== sample minutes (16:11-16:15, 17:00-17:04):")
for key in sorted(per_min):
    day, hhmm = key
    if ("16:11" <= hhmm <= "16:15") or ("17:00" <= hhmm <= "17:04"):
        t, p, r, s = per_min[key]
        print(f"{hhmm}: total={t} poll={p} release={r} start={s}")

# средний интервал между стартами в пике (для таймера симулятора)
print("== peak starts total:", sum(vals))
