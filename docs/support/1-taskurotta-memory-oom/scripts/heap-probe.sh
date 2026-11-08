#!/usr/bin/env bash
# heap-probe.sh — общий снимок хепа из любого hprof-дампа: HZ-мапы
# (записи/retained по каждой мапе, локальные heapCost, max-size-политики),
# бизнес-объекты (Process/Task/TaskDecision/Graph), потоки (группы, состояния,
# dw-треды), AQS-лок с ожидателями, retained-кандидаты — аттрибуция «остатка»
# хепа после среза метрик (шаг 5 сценария кейса 1).
#
# Использование:
#   heap-probe.sh <дамп.hprof>
#
# Переменные окружения (дефолты — ~/opt):
#   MAT_JDK_HOME — JDK 21 для запуска (скрипт и MAT 1.17 требуют JDK 21)
#                  (дефолт: $HOME/opt/jdk-21.0.13+9-temurin)
#   MAT_HOME     — Eclipse MAT 1.17; classpath — все plugin-джары из
#                  $MAT_HOME/plugins/ (дефолт: $HOME/opt/mat-1.17)
#
# Исходник HeapProbe.java лежит рядом со скриптом; перед запуском
# компилируется во временный каталог (удаляется по завершении).
set -euo pipefail

usage() {
  echo "Usage: $(basename "$0") <dump.hprof>" >&2
  exit 2
}

[ "$#" -eq 1 ] || usage
dump="$1"
[ -f "$dump" ] || { echo "ERROR: dump not found: $dump" >&2; exit 1; }

MAT_JDK_HOME="${MAT_JDK_HOME:-$HOME/opt/jdk-21.0.13+9-temurin}"
MAT_HOME="${MAT_HOME:-$HOME/opt/mat-1.17}"

java_bin="$MAT_JDK_HOME/bin/java"
javac_bin="$MAT_JDK_HOME/bin/javac"
[ -x "$java_bin" ] || { echo "ERROR: java not found: $java_bin (set MAT_JDK_HOME)" >&2; exit 1; }
[ -d "$MAT_HOME/plugins" ] || { echo "ERROR: MAT plugins dir not found: $MAT_HOME/plugins (set MAT_HOME)" >&2; exit 1; }

script_dir="$(cd -- "$(dirname -- "$0")" && pwd)"
src="$script_dir/HeapProbe.java"
[ -f "$src" ] || { echo "ERROR: source not found: $src" >&2; exit 1; }

# classpath: все plugin-джары MAT (каталог сборки добавляется ниже)
cp_entries=""
for j in "$MAT_HOME"/plugins/*.jar; do
  cp_entries="${cp_entries}${j}:"
done

build_dir="$(mktemp -d "${TMPDIR:-/tmp}/heap-probe.XXXXXX")"
trap 'rm -rf "$build_dir"' EXIT

"$javac_bin" -cp "${cp_entries}" -d "$build_dir" "$src"

"$java_bin" -Xmx12g -cp "${cp_entries}${build_dir}" HeapProbe "$dump"
