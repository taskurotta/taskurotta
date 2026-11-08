#!/usr/bin/env bash
# Снимок hprof из e2e-taskurotta (pid 1) + выгрузка на хост.
# Использование: dump.sh <label> [каталог-приёмник (по умолчанию ./dumps)]
set -euo pipefail
LABEL="${1:?label required}"
TS="$(date -u +%Y%m%dT%H%M%SZ)"
CONTAINER="e2e-taskurotta"
DEST_DIR="${2:-dumps}"
mkdir -p "$DEST_DIR" /tmp/javadumps
REMOTE="/tmp/javadumps/e2e-baseline-${LABEL}-${TS}.hprof"
docker exec "$CONTAINER" mkdir -p /tmp/javadumps
docker exec "$CONTAINER" jmap -dump:format=b,file="$REMOTE" 1
HOST_FILE="$DEST_DIR/e2e-baseline-${LABEL}-${TS}.hprof"
docker cp "$CONTAINER:$REMOTE" "$HOST_FILE"
SIZE="$(stat -c%s "$HOST_FILE")"
SHA="$(sha256sum "$HOST_FILE" | awk '{print $1}')"
echo "DUMP_OK label=$LABEL file=$HOST_FILE size=$SIZE sha256=$SHA"
