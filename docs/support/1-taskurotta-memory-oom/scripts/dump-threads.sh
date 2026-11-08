#!/usr/bin/env bash
# dump-threads.sh — дамп потоков JVM (jstack, PID 1) + анализ зомби-состояния
# (шаг 3 сценария кейса 1):
#  - «Found N deadlock(s)» и число «waiting to lock» (мониторы);
#  - группировка «parking to wait for <адрес>»: сколько потоков стоят на
#    каждом лок-адресе и есть ли владелец в дампе («locked <адрес>»);
#    ожидатели на одном адресе при отсутствии владельца — сигнатура зомби
#    (владелец лока погиб, лок не освободится автоматически);
#  - группировка потоков по именам (dw-*, cached*, hz partition-operation,
#    GC-N, Metrics-job-N, metrics-meter-tick-thread-* и др.);
#  - turnover: максимальный номер потока vs число потоков в дампе.
#
# Использование:
#   dump-threads.sh <контейнер> [файл-дампа]
#
# Файл-дампа (дефолт: ./thdump.txt) пишется на хост; анализ выводится в
# stdout и сохраняется в <файл-дампа>.analysis.txt.
# Альтернатива jstack: kill -3 → строки дампа в docker json-log (docker logs).
set -euo pipefail

usage() {
  echo "Usage: $(basename "$0") <container> [thdump-file]" >&2
  exit 2
}

[ "$#" -ge 1 ] && [ "$#" -le 2 ] || usage
container="$1"
out="${2:-thdump.txt}"
analysis="${out}.analysis.txt"

command -v docker >/dev/null || { echo "ERROR: docker not found" >&2; exit 1; }
docker inspect "$container" >/dev/null 2>&1 || { echo "ERROR: container not found: $container" >&2; exit 1; }

echo "docker exec $container jstack 1 > $out"
docker exec "$container" jstack 1 > "$out"
echo "thread dump: $out ($(wc -l < "$out") lines)"

# deadlocks и мониторные ожидания (grep -c возвращает 1 при нуле — гасим)
deadlock_line="$(grep -E 'Found [0-9]+ deadlock' "$out" || true)"
monitor_waits="$(grep -c 'waiting to lock' "$out" || true)"

{
  echo "thread dump analysis: $out"
  echo "deadlocks: ${deadlock_line:-<no 'Found N deadlocks' line>}"
  echo "monitor waits ('waiting to lock'): ${monitor_waits:-0}"
  echo
  awk '
    function prefix(name) {
      sub(/[-_][0-9]+$/, "", name)
      return name
    }
    /^"/ {
      total++
      rest = substr($0, 2)
      q = index(rest, "\"")
      nm = ""
      if (q > 1) {
        nm = substr(rest, 1, q - 1)
        groups[prefix(nm)]++
      }
      cur = nm
      if (match($0, /" #[0-9]+/)) {
        num = substr($0, RSTART + 3, RLENGTH - 3) + 0
        if (num > maxnum) maxnum = num
      }
      next
    }
    $0 ~ /parking to wait for/ && match($0, /<0x[0-9a-fA-F]+>/) {
      addr = substr($0, RSTART, RLENGTH)
      waiters[addr]++
      if (cur != "") wsample[addr] = cur
      next
    }
    match($0, /locked <0x[0-9a-fA-F]+>/) {
      s = substr($0, RSTART, RLENGTH)
      owner[substr(s, 8)] = 1
      next
    }
    END {
      printf "threads total: %d\n", total
      printf "max thread number: #%d\n", maxnum
      if (total > 0 && maxnum > total * 2)
        print "turnover: max# заметно больше числа потоков — потоки создаются и умирают"
      print ""
      print "-- waiters per lock address (top) --"
      n = 0
      for (a in waiters) keys[++n] = a
      for (i = 1; i <= n; i++)
        for (j = i + 1; j <= n; j++)
          if (waiters[keys[j]] > waiters[keys[i]]) { t = keys[i]; keys[i] = keys[j]; keys[j] = t }
      for (i = 1; i <= n; i++) {
        a = keys[i]
        if (a in owner)
          printf "%6d  %s  owner in dump: YES (locked %s)  e.g. %s\n", waiters[a], a, a, wsample[a]
        else
          printf "%6d  %s  owner in dump: NO (зомби-сигнатура: владелец лока погиб)  e.g. %s\n", waiters[a], a, wsample[a]
      }
      if (n == 0) print "(no parking waiters in dump)"
      print ""
      print "-- thread groups (by name prefix, top 25) --"
      m = 0
      for (g in groups) gkeys[++m] = g
      for (i = 1; i <= m; i++)
        for (j = i + 1; j <= m; j++)
          if (groups[gkeys[j]] > groups[gkeys[i]]) { t = gkeys[i]; gkeys[i] = gkeys[j]; gkeys[j] = t }
      limit = (m > 25) ? 25 : m
      for (i = 1; i <= limit; i++)
        printf "%6d  %s\n", groups[gkeys[i]], gkeys[i]
    }
  ' "$out"
} | tee "$analysis"

echo
echo "analysis saved: $analysis"
