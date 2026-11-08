#!/usr/bin/env python3
"""Сборщик heap по времени: GET /admin/metrics каждые --interval сек -> JSONL.
Запись: {t, ts, heap_used, heap_committed, heap_max, gc_count, gc_time, active_requests, err5xx}"""
import argparse
import json
import time
import urllib.request


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--admin", default="http://localhost:8815/admin/metrics")
    p.add_argument("--out", required=True)
    p.add_argument("--interval", type=float, default=30)
    args = p.parse_args()
    t0 = time.time()
    with open(args.out, "a") as f:
        while True:
            try:
                with urllib.request.urlopen(args.admin, timeout=10) as r:
                    d = json.load(r)
                g = d["gauges"]
                rec = {
                    "t": int(time.time() - t0),
                    "ts": time.strftime("%Y-%m-%dT%H:%M:%S"),
                    "heap_used": g["jvm.memory.heap.used"]["value"],
                    "heap_committed": g["jvm.memory.heap.committed"]["value"],
                    "heap_max": g["jvm.memory.heap.max"]["value"],
                    "gc_count": g["jvm.gc.PS-Scavenge.count"]["value"]
                    + g["jvm.gc.PS-MarkSweep.count"]["value"],
                    "gc_time_ms": g["jvm.gc.PS-Scavenge.time"]["value"]
                    + g["jvm.gc.PS-MarkSweep.time"]["value"],
                    "active_requests": d["counters"].get(
                        "io.dropwizard.jetty.MutableServletContextHandler.active-requests",
                        {}).get("count", 0),
                    "err5xx": d["meters"].get(
                        "io.dropwizard.jetty.MutableServletContextHandler.5xx-responses",
                        {}).get("count", 0),
                }
                f.write(json.dumps(rec) + "\n")
                f.flush()
            except Exception as e:
                print("heapwatch error:", e, flush=True)
            time.sleep(args.interval)


if __name__ == "__main__":
    main()
