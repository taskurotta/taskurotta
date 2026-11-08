#!/usr/bin/env bash
# dump-heap.sh — сборка хеп-дампа инстанса taskurotta в контейнере Docker
# (шаг 2 сценария кейса 1).
#
# Процедура:
#  1. Проверка типа монтировки директории дампов: bind-mount (дампы живут
#     на хосте) vs именованный том (дампы терятся при пересоздании
#     контейнера — предупреждение; стойкая мера: bind-mount на хост).
#  2. Если в директории уже есть хеп-дамп (OOME, флаг
#     -XX:+HeapDumpOnOutOfMemoryError в JVM-команде) — выгрузка на хост
#     через docker cp (fallback; live-дамп с зомби-инстанса может не снять).
#  3. Иначе — живой снимок: jmap -dump:live в контейнере (PID 1 — JVM;
#     jmap присутствует в образе). Грабли: jmap зависает (GC-спираль) —
#     повторить без :live.
#
# Использование:
#   dump-heap.sh <контейнер> <путь-дампов-в-контейнере> [имя-дампа]
#
# Примеры:
#   dump-heap.sh taskurotta /tmp/javadumps
#   dump-heap.sh taskurotta /tmp/javadumps oom-analysis
#
# Переменные окружения:
#   DUMP_DIR — каталог на хосте для выгрузки найденного OOME-дампа
#              (дефолт: ./heapdumps)
set -euo pipefail

usage() {
  echo "Usage: $(basename "$0") <container> <container-dump-path> [dump-name]" >&2
  exit 2
}

[ "$#" -ge 2 ] && [ "$#" -le 3 ] || usage
container="$1"
dump_path="${2%/}"
dump_name="${3:-heapdump}"
host_dir="${DUMP_DIR:-./heapdumps}"

command -v docker >/dev/null || { echo "ERROR: docker not found" >&2; exit 1; }
docker inspect "$container" >/dev/null 2>&1 || { echo "ERROR: container not found: $container" >&2; exit 1; }

echo "=== 1. mount type check: $dump_path ==="
fmt="{{range .Mounts}}{{if eq .Destination \"$dump_path\"}}{{.Type}}{{end}}{{end}}"
mount_type="$(docker inspect -f "$fmt" "$container")"
case "$mount_type" in
  bind)
    echo "OK: $dump_path — bind-mount (дампы живут на хосте)"
    ;;
  volume)
    echo "WARNING: $dump_path — именованный том: дампы теряются при пересоздании контейнера."
    echo "         Стойкая мера: bind-mount директории дампов на хост."
    ;;
  *)
    echo "WARNING: монтировка для $dump_path не найдена (локальный путь контейнера?"
    echo "         дамп живёт, пока жив контейнер)"
    ;;
esac

echo
echo "=== 2. existing hprof in $dump_path ==="
existing="$(docker exec "$container" sh -c "ls -1 \"$dump_path\"/*.hprof 2>/dev/null || true")"
if [ -n "$existing" ]; then
  echo "found existing hprof (OOME dump):"
  echo "$existing"
  echo "copying to host dir: $host_dir (docker cp fallback)"
  mkdir -p "$host_dir"
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    docker cp "$container:$f" "$host_dir/"
    echo "  copied: $f -> $host_dir/"
  done <<< "$existing"
  echo "DONE: existing dump extracted, live dump skipped"
  exit 0
fi
echo "no hprof in $dump_path"

echo
echo "=== 3. live dump: jmap -dump:live, PID 1 ==="
target="$dump_path/$dump_name.hprof"
echo "docker exec $container jmap -dump:live,format=b,file=$target 1"
docker exec "$container" jmap -dump:live,format=b,file="$target" 1
docker exec "$container" sh -c "ls -l \"$target\""
echo "DONE: $target (in container $container)"
