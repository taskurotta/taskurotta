#!/usr/bin/env bash
# collect-logs.sh — выгрузка сигнатур логов (паттерны шага 1 сценария кейса 1)
# в рабочую папку разбора:
#   - docker json-log контейнера (stdout/stderr): строки OOME — текст OOME
#     живёт именно там, а не в приложенном логе (редирект &> в command
#     compose-образа docker'ом разбивается на argv и не применяется);
#   - service.log: heartbeat-разрывы («System clock apparently jumped»),
#     медленные операции HZ («Slow operation detected»),
#     'is-executing': false, «Aborting invocation»;
#   - request.log: максимальная латентность (последнее поле строки — мс),
#     5xx (9-е поле — статус).
#
# Использование:
#   collect-logs.sh <output-dir> <контейнер> [service-log] [request-log]
#
# Пути service-log / request-log — В КОНТЕЙНЕРЕ (из актуальной конфигурации
# деплоя; в git-сборке — ./target/logs/). Источник без аргумента пропускается
# с заметкой. Вывод — отдельные файлы с номерами строк в <output-dir>/.
#
# Пример:
#   collect-logs.sh <output-dir> taskurotta \
#     /app/target/logs/service.log /app/target/logs/request.log
set -euo pipefail

usage() {
  echo "Usage: $(basename "$0") <output-dir> <container> [service-log] [request-log]" >&2
  exit 2
}

[ "$#" -ge 2 ] && [ "$#" -le 4 ] || usage
out_dir="$1"
container="$2"
service_log="${3:-}"
request_log="${4:-}"

command -v docker >/dev/null || { echo "ERROR: docker not found" >&2; exit 1; }
docker inspect "$container" >/dev/null 2>&1 || { echo "ERROR: container not found: $container" >&2; exit 1; }
mkdir -p "$out_dir"

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/collect-logs.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

echo "=== docker json-log: OOME -> $out_dir/oome.txt ==="
docker logs "$container" 2>&1 | grep -n 'OutOfMemoryError' > "$out_dir/oome.txt" || true
echo "lines: $(wc -l < "$out_dir/oome.txt")"

if [ -n "$service_log" ]; then
  echo "=== service.log ($service_log) ==="
  if docker exec "$container" cat "$service_log" > "$work_dir/service.log" 2>/dev/null; then
    grep -n 'System clock apparently jumped' "$work_dir/service.log" > "$out_dir/clock-jump.txt" || true
    grep -n 'Slow operation detected' "$work_dir/service.log" > "$out_dir/slow-operations.txt" || true
    grep -n "'is-executing': false" "$work_dir/service.log" > "$out_dir/is-executing-false.txt" || true
    grep -n 'Aborting invocation' "$work_dir/service.log" > "$out_dir/aborting-invocation.txt" || true
    for f in clock-jump slow-operations is-executing-false aborting-invocation; do
      echo "  $f.txt: $(wc -l < "$out_dir/$f.txt") lines"
    done
  else
    echo "WARNING: cannot read $service_log in container $container (файл нет? путь неверный?) — service.log пропущен"
  fi
else
  echo "service.log: не задан — пропущен"
fi

if [ -n "$request_log" ]; then
  echo "=== request.log ($request_log) ==="
  if docker exec "$container" cat "$request_log" > "$work_dir/request.log" 2>/dev/null; then
    awk '$NF+0>m{m=$NF+0} END{print "max_response_ms="m}' "$work_dir/request.log" > "$out_dir/request-max-ms.txt"
    awk '$9 ~ /^5/' "$work_dir/request.log" > "$out_dir/request-5xx.txt" || true
    echo "  $(cat "$out_dir/request-max-ms.txt")"
    echo "  request-5xx.txt: $(wc -l < "$out_dir/request-5xx.txt") lines"
  else
    echo "WARNING: cannot read $request_log in container $container (файл нет? путь неверный?) — request.log пропущен"
  fi
else
  echo "request.log: не задан — пропущен"
fi

echo "DONE: signatures in $out_dir/"
