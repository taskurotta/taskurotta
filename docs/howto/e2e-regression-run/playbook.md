# How-to: Регрессионный E2E-прогон (e2e-regression-run)

## Назначение
Рецепт регрессионного E2E-прогона taskurotta: запуск движка «как на площадке»
(боевой деплой-конфиг), симуляция 363 актёров по профилю пика
инцидента, серия heap-дампов и отчёт «корректность + память метрик». Прогон
под длительным трафиком: в перспективе — более интенсивный и настраиваемый
трафик, память и поведение продукта на большой нагрузке — всё за один проход.
Эталон для сравнения будущих сборок — `baseline.md` (зафиксированный результат). Границы:
плейбук — процесс (рецепт и инструменты), а не описание состояния системы:
что такое поведение памяти метрик и почему она растёт — в домене metrics.

## Зависимости (домены/компоненты)
- → [metrics](../../domains/metrics/domain.md) — отчёт измеряет память подсистемы
  метрик: эталонные значения и сравнительная таблица «до/после» сопоставляются
  с поведением метрик.

## Связанные документы
- [metrics-subsystem](../../domains/metrics/components/metrics-subsystem.md) —
  компонент: сбор и хранение рядов метрик в памяти процесса.
- [project-environment](../project-environment/playbook.md) — окружение проекта:
  локальные compose-среды, базы данных, логи.

---

Свободная зона (состав разделов — на усмотрение создателя).

## Состав плейбука
- `files/` — стек «как на площадке»: `docker-compose-deps-local.yml`
  (postgres 16.5), `docker-compose-deps.yml` (pgbouncer 1.23.0),
  `docker-compose-taskurotta.yml` (taskurotta 12.5 + mongo 3.7.9), `mongodb.conf`,
  `.env.example` (шаблон env, только плейсхолдеры), `userlists.txt` (шаблон
  userlist pgbouncer, только плейсхолдеры).
- `scripts/` — обвязка: `simulator.py` (нагрузка), `heapwatch.py` (кривая heap),
  `dump.sh` (jmap-дампы), `logstats.py` (поминутный разрез request.log),
  `profile_peak.json` (поминутный профиль стартов пика инцидента: 110 минут,
  всего 7414).
- Секреты в git — только плейсхолдеры: `.env` и реальный `userlists.txt` —
  локальные файлы, не коммитятся.

## Протокол API (как симулятор управляет сервером)
Эндпоинты (порты: хост 8815 -> контейнер 8811):
- `POST /rest/tasks/start` — тело TaskContainer. Тип начальной задачи — только
  `DECIDER_START` или `WORKER_SCHEDULED`; сервер сам подставляет `startTime`
  и UUID, если отсутствуют.
- `POST /rest/tasks/poll` — тело `{"name": ..., "version": ...}` (ActorDefinition,
  `taskList` опционален). Long-poll до 60 с (`server.poll-delay: 60 seconds`):
  204 = задач нет, 200 = TaskContainer. HZ-очередь актёра — `task_<actorId>`.
- `POST /rest/tasks/release` — тело DecisionContainer: массив `tasks` расширяет
  граф процесса; новых задач нет — процесс завершён.
- `GET /rest/monitoring/stats` — счётчики started/finished/broken, размеры HZ-карт.
- `actorId` = `name#version` (в этом плейбуке: `e2e.a<N>#1.0`).

Wire-формат (JSON, установлен интроспекцией Jackson сервера 12.5):
- ArgContainer: `dataType, taskId, valueType (PLAIN|ARRAY|COLLECTION), compositeValue,
  promise, errorContainer, ready, jsonvalue`. Поле — именно `jsonvalue`
  (legacy-mangling Jackson: `JSONValue`/`jsonValue` -> 400).
- TaskContainer: `taskId, processId, pass, method, actorId, type (имя enum),
  startTime, errorAttempts, args, options, unsafe, failTypes`.
- DecisionContainer: `taskId, processId, pass, value (ArgContainer), errorContainer,
  restartTime (-1 = NO_RESTART), tasks (TaskContainer[]), actorId, executionTime`.

## Стек: подъём
Сеть `topnet` — external; если на машины её нет: `docker network create topnet`.
Алиасы на topnet: `taskurotta` и `mongodb` — благодаря им боевой конфиг
(`hz-postgres.yml`: `mongo.client-uri: mongodb://mongodb:27017`,
`hz.nodes: taskurotta:7777`) работает без правок.

| Контейнер | Образ | Назначение |
|---|---|---|
| e2e-top-db16 | repository.technokad.ru:5000/postgres:16.5 | Postgres, хост-порт 5434:5432, том `e2e-top-db16` |
| e2e-pgbouncer | repository.technokad.ru:5000/pgbouncer:1.23.0 | Пуллер, порт 6432, transaction-pooling |
| e2e-taskurotta | repository.technokad.ru:5000/taskurotta:12.5 | Движок, хост 8815 -> контейнер 8811, `-Xmx1024m` |
| e2e-mongodb | repository.technokad.ru:5000/mongo:3.7.9 | Хранилище очередей/IMap, порт 27017, `ulimits nofile 65536` (дефолтный docker-лимит 1024 исчерпывается на пике: «Too many open files», SIGABRT — причина в комментарии compose) |

Боевые конфиги сервера (каталог `server/` с `hz-postgres.yml`) и job-store
планировщика (каталог `scheduler/` с `1.json`–`5.json`) в `files/` не входят:
они пробрасываются в контейнер из локальных каталогов по переменным
`TASKUROTTA_CONFIG_DIR` / `TASKUROTTA_SCHEDULER_DIR` (источник и обновление —
раздел «Процедура обновления конфигурации»).

Подъём (рабочий каталог — вне `docs/`, например `e2e-run/`):
```
mkdir -p e2e-run && cd e2e-run
cp ../docs/howto/e2e-regression-run/files/* .
cp ../docs/howto/e2e-regression-run/files/.env.example .
mkdir -p scripts && cp ../docs/howto/e2e-regression-run/scripts/* scripts/
cp .env.example .env && $EDITOR .env          # заполнить по рецепту ниже
docker network create topnet                  # если сети ещё нет
docker compose -f docker-compose-deps-local.yml \
               -f docker-compose-deps.yml \
               -f docker-compose-taskurotta.yml up -d
```

## env: как заполнить (локальные e2e-учётки)
Рецепт — всё локальное, придуманное под проверку; площадочных значений нет:
1. Придумать локальный e2e-пароль (любая сильная строка) — одно и то же значение
   в `TASKUROTTA_DB_PASS`, `APP_DB_PASSWORD`, `PGBOUNCER_POSTGRES_PASS`.
2. `APP_DB_USERNAME` — имя суперпользователя postgres, создаваемого контейнером.
3. `TASKUROTTA_DB_USER=postgres` — рабочая учётка. Факт о стеке: bitnami-входная
   точка pgbouncer генерирует свой userlist из env (пользователь `postgres`,
   пароль `POSTGRESQL_PASSWORD`) и смонтированный `userlists.txt` игнорирует;
   роль `postgres` — SUPERUSER, владелец БД taskurotta. Шаблон `userlists.txt`
   нужен для образов pgbouncer, читающих `/bitnami/userlists.txt`.
4. `TASKUROTTA_DB_URL=jdbc:postgresql://e2e-pgbouncer:6432/taskurotta` — без правок.
5. `PGBOUNCER_MAX_PREPARED_STATEMENTS=128` — обязательно `>0`: при `0` в
   transaction-пуллинге теряются prepared statements (500 «prepared statement
   does not exist» на `POST /rest/tasks/start`).
6. `TASKUROTTA_CONFIG=hz-postgres.yml`; `TASKUROTTA_CONFIG_DIR` /
   `TASKUROTTA_SCHEDULER_DIR` — абсолютные пути к локальным копиям конфигов
   деплоя (см. «Процедура обновления конфигурации»).
7. `E2E_LOG_DIR` — каталог логов стека; создать до старта:
   `mkdir -p "$E2E_LOG_DIR"/taskurotta "$E2E_LOG_DIR"/postgres`.

БД: postgres-контейнер создаёт базы `application`, `auth`, `taskurotta`, `files`.
Схему taskurotta приложение НЕ создаёт (в сборке 12.5 нет auto-migration) —
применить вручную из `services-pg/src/main/resources/sql/create.sql`
(7 таблиц) — то, что на площадке делает DBA до холодного старта:
```
docker exec -i e2e-top-db16 psql -U postgres -d taskurotta < services-pg/src/main/resources/sql/create.sql
```

## Симулятор
- `scripts/simulator.py` — N актёров = N потоков, у каждого один in-flight
  long-poll (новое соединение на запрос, как у реального клиента). Задача
  получена -> «размышление» (экспонента, среднее 60 с, cap 300 с) -> release
  с следующей задачей или завершение. План процесса регистрируется ДО
  `POST /start`: сервер ставит задачу в очередь в момент запроса, иначе
  long-poll забрал бы задачу до записи плана («no-plan»).
- `scripts/profile_peak.json` — поминутный профиль стартов пика инцидента
  (110 минут, 7414 стартов, max ~279/мин, среднее 67.4/мин); как он получен —
  `scripts/logstats.py`.
- Параметры полного прогона (профиль пика инцидента):
  `--actors 363 --profile scripts/profile_peak.json --warmup-min 10 --tail-min 20
  --bg-rate 1.9 --thinking-mean 60 --thinking-max 300 --drain-min 15`
  — warmup 10 мин (фон 1.9/мин) + пик 110 мин + tail 20 мин + дренаж 15 мин:
  ~140 мин + дренаж, ~7450 процессов.
- Счётчики: polls 200/204, латентность poll, starts/releases/finished, 5xx,
  live-процессы; снимки каждые 10 с — `counters.jsonl`, итог — `summary.json`
  в каталоге `--out`.
- Зафиксированные допущения (отклонения от реальных клиентов): время
  «размышления» на площадке не измерялось; 363 актёра с равномерным
  распределением; методы задач фиксированы ("start"/"work"), аргумент — одна
  строка; один long-poll на актёра.
- Калибровочный (малый) прогон: `--actors 12 --rate 2.2 --duration-min 15`
  (без `--profile`).

## Полный прогон (рецепт)
Предусловие: чистый старт (раздел ниже) и свежий JVM. Рабочий каталог — `e2e-run/`
(compose + `.env` + `scripts/`, см. «Стек: подъём»).

```
# 1. Свежий JVM — t=0
docker restart e2e-taskurotta
# Ждём Jetty UP: docker exec e2e-taskurotta wget -q -O /dev/null --timeout=3 http://localhost:8811/admin/
# 2. Чистые таблицы процессов для финальной сверки
docker exec -i e2e-top-db16 psql -U postgres -d taskurotta \
  -c "TRUNCATE TSK_PROCESS, TSK_PROCESS_IDEMPOTENCY;"
mkdir -p run dumps
# 3. Кривая heap каждые 30 с (фоновый)
nohup python3 scripts/heapwatch.py --out run/heap.jsonl --interval 30 > run/heapwatch.log 2>&1 &
HPID=$!
# 4. Дампы на t=15/45/60/120 мин (фоновый)
( sleep 900;  bash scripts/dump.sh t15min dumps
  sleep 1800; bash scripts/dump.sh t45min dumps
  sleep 900;  bash scripts/dump.sh t60min_peak dumps
  sleep 3600; bash scripts/dump.sh t120min dumps ) > run/dumps.log 2>&1 &
DPID=$!
# 5. Нагрузка (фронт)
python3 scripts/simulator.py --actors 363 --out run \
  --profile scripts/profile_peak.json \
  --warmup-min 10 --tail-min 20 --bg-rate 1.9 \
  --thinking-mean 60 --thinking-max 300 --drain-min 15 > run/sim.log 2>&1
# 6. После выхода симулятора
bash scripts/dump.sh end dumps
kill $HPID $DPID 2>/dev/null || true
curl -s http://localhost:8815/rest/monitoring/stats > run/final-stats.txt
docker exec -i e2e-top-db16 psql -U postgres -d taskurotta \
  -c "select state, count(*) from TSK_PROCESS group by state order by state;" > run/final-db.txt
```
Дампы хранятся в `e2e-run/dumps/` (jmap выполняется в `e2e-taskurotta` pid 1,
затем `docker cp`); ожидаемая длительность ~165 мин + MAT-анализ.

## Замеры
- Кривая heap: `heapwatch.py` каждые 30 с по `/admin/metrics` — heap
  used/committed/max, GC count/time, активные запросы, счётчик 5xx -> JSONL.
- Дампы: точки `t15min`, `t45min`, `t60min_peak`, `t120min`, `end`.
- MAT-анализ — ОДИНАКОВАЯ методика для всех дампов (dominant retained set):
  - `MetricsDataHandler.retained` — метрические строки/кольца + всё, что они
    удерживают;
  - `NumberDataHandler.retained` — то же для number-метрик;
  - сверка через гистограммы: n / shallowSum `DataRowVO`, `NumberDataRowVO`,
    `DataPointVO` + длины колец;
  - HZ-карты (`Task`, `TaskDecision`, `Graph`) — retained по ключевым объектам,
    для контекста.
- Инструменты: MAT 1.17 и JDK 21 (Temurin) — `~/opt/mat-1.17`,
  `~/opt/jdk-21.0.13+9-temurin`. Если на машине их нет — спросить владельца;
  самостоятельно не устанавливать.

## Чистый старт (обязательно перед каждым новым прогоном)
HZ-очереди и IMap персистятся в mongo и подхватываются новым JVM при старте —
старый state исказит ключи и память метрик. Процедура:
1. mongo, БД `taskurotta`: удалить коллекции `task_*` (очереди актёров),
   `dq_task_*` (delay-очереди по актёрам) и HZ IMap `Task`, `TaskDecision`,
   `Graph`, `ActorPreferencesMap`; `dq_gc_GarbageCollector` (метаданные GC
   delay-очередей) и `Recovery` не трогаются:
```
docker exec e2e-mongodb mongo taskurotta --quiet --eval "
  db.getCollectionNames().forEach(function (n) {
    if (n.startsWith('task_') || n.startsWith('dq_task_') ||
        n === 'Task' || n === 'TaskDecision' || n === 'Graph' ||
        n === 'ActorPreferencesMap') db[n].drop();
  });"
```
2. Postgres: `TRUNCATE TSK_PROCESS, TSK_PROCESS_IDEMPOTENCY;`.
3. Один симулятор на сервер: два симулятора с одинаковым пространством актёров
   забирают long-poll задачи друг друга (массовые «no-plan», прогон невалиден).
 4. Fat jar — только с `clean` (`mvn -o clean package -DskipTests`): без clean
    maven-shade мержит зависимости поверх старой базы `original-*.jar` от
    прежней сборки, и устаревшие классы переживают пересборку. После сборки
    сверить sha256 изменённых классов внутри jar; классы из jar извлекать
    локально (docker cp jar + unzip на хосте) — прямой `unzip -p … > файл`
    по пайпу может усечь файл, и проверка по усечённой копии даст ложный
    результат.

## Формат отчёта
- Корректность — таблица «проверка / факт / статус»: стартов (vs профиль),
  finished % (серверный счётчик + БД `TSK_PROCESS state=1`), broken, 5xx
  (счётчик клиента + request.log с t=0), ERROR в service.log с t=0, OOME
  (docker logs), латентность poll p50/p99/max (лимит 60 с), решений на
  процесс, live-процессы после дренажа.
- Снимок консоли: в t=120 мин `GET /rest/console/metrics/data`
  (`type=rate`, `period=1hour` и `24hours`, датасет-агрегат = имя метрики) —
  сумма значений за окно по каждому датасету; сопоставить со счётчиками
  симулятора (`counters.jsonl`, шаг 10 с): отклонение сумм — не более одного
  интервала бина на датасет: ≤ 10 с (час) / ≤ 2 мин (сутки) (консоль отдаёт
  только полные бины; недополненный бин в сумме «всего за окно» учитывается).
  Маппинг метрик на счётчики симулятора: `successfulPoll` — `polls200` (1:1);
  `enqueue` — `starts + releases - finished`: сервер штампует вставку в очередь
  на каждую вставку (старт процесса вставляет первую задачу, каждый срединный
  release вставляет следующую задачу плана, финальный release с `tasks=None`
  ничего не вставляет).
- Память — сравнительная таблица по каждому дамп, колонки t15/t45/t60/t120/end:
  MetricsDataHandler retained, NumberDataHandler retained, метрики итого,
  доля метрик в used heap, число ключей (n(CheckPoint)), строки час/день,
  DataPointVO n (shallow), heap_used. Для «до/после» — два блока значений
  (эталон ниже и новая сборка); критерий: plateau метрик не растёт, доля
  метрик в used heap падает.
- Все цифры — MAT (методика выше) + `/admin/metrics` (heapwatch); дампы
  нумеруются «имя / размер / sha256 / t (мин)».

## Эталонные значения (сборка 12.5, неизменённая)
Корректность (полный прогон, 363 актёра, профиль пика):
- 7471 стартов (профиль сохранён: 7414 + фон warmup/tail), 7466 finished =
  99.93%, broken = 0.
- 5 процессов state=0 — артефакт дренажа (худший цикл жизни процесса ≈ 25 мин
  > дренаж 15 мин), сбой сервера не подтверждён.
- 5xx = 0, OOME = 0, ERROR в service.log = 0.
- poll: n = 35921, p50 = 2 мс, p99 = 56.8 с, max = 60 с (лимит long-poll
  соблюдён).
- Решения: 35916 = 4.81 на процесс (инцидент — 4.6).
- GC к концу: ~1296 сборок, ~12% wall-clock в последний час; пик heap_used
  962 МБ при Xmx 1024m.

Память (метрики — основной потребитель кучи):

| t (мин) | Метрики, МБ (MDS + NDS) | Ключи | Метрики / used heap |
|---|---|---|---|
| 15 | 88.3 (61.9 + 26.4) | 692 | 37% |
| 45 | 443.4 (413.3 + 30.1) | 2184 | 78% |
| 60 (пик) | 589.2 (557.5 + 31.7) | 2184 | 78% |
| 120 | 708.5 (670.4 + 38.1) | 2184 | 88% |
| end (164) | 718.8 (676.0 + 42.8) | 2184 | 89% |

- Keyspace 2184 = 363 актёра × 6 метрик + 6 агрегатных (двойная отметка в коде:
  актёр+метод и имя метрики); метрика `errorDecision` не создаётся — ошибок в
  прогоне не было.
- Рост — по wall-clock, не по нагрузке: после plateau ключей (t45) метрики
  растут при падающей нагрузке (589 -> 708 -> 719 МБ); часовое кольцо полно
  через ~1 час.
- Справка (дамп инцидента, та же методика): 687.7 МБ (509.1 + 178.6), 1611
  ключей — полный plateau инцидента повторён и превзойдён двухчасовым прогоном.
- 24-часовая оценка для e2e-ключей (линейная экстраполяция по измеренной
  маржинальной стоимости): кольца ~28.3 М точек, метрики ≈ 1.1 ГБ > Xmx 1024m —
  без исправления OOME в пределах суток.

## Процедура обновления конфигурации
- Источник конфигурации стека (compose, каталог `server/`, job-store
  `scheduler/`) — боевой деплой, только чтение.
- При расхождении между деплоем и копиями в этом плейбуке: взять актуальную
  версию из деплоя, пересобрать локальные копии (`files/` и каталоги,
  проброшенные в контейнер), перепроверить подъём и зафиксировать расхождение
  в отчёте задачи.
- sha и дата деплоя в плейбуке не фиксируются: деплой меняется, и значения
  в git устаревают.
- Площадочные секреты (do.env, local.env, площадочный userlists.txt) не
  читаются, не копируются, не коммитятся: в git только шаблоны `.env.example`
  и `userlists.txt`.
