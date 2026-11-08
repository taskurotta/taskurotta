# Кейс: Проблемы памяти инстанса taskurotta: OOME, GC-спираль, plateau памяти, зомби-состояние после OOME

**Case**: 1
**Status**: READY (2026-11-06)

Состояние — одно из трёх (слово + дата, при CLOSED — свободная причина); смена —
владельцем, либо агентом по слову владельца:
- `ANALYZING (YYYY-MM-DD)` — кейс зафиксирован, сценария нет (файл создан, чтобы
  проработать и зафиксировать);
- `READY (YYYY-MM-DD)` — исполняемый сценарий готов (кейс, созданный по слову
  владельца уже со сценарием, — READY сразу);
- `CLOSED (YYYY-MM-DD, <причина — свободная>)` — после CLOSED папка переезжает
  в `docs/support/archive/`.

## Симптом

«Сервис не отвечал». Инстанс taskurotta перестаёт отдавать ответы на запросы
(оператор видит: poll/мониторинг не возвращают результат, таймауты), при этом хеп
JVM уходит в потолок и GC начинает бурлить. Варианты манифестации одного типа
проблемы: OOME (`OutOfMemoryError: Java heap space`), GC-спираль (паузы
остановки десятков секунд, деградация латентности), plateau памяти (метрическая
подсистема занимает ~50–90% живого хепа) и медленный рост хепа со временем
работы процесса. После OOME инстанс может перейти в зомби-состояние: JVM жив,
сервис не отвечает часами.

## Быстрая диагностика (сценарий)

Среда и инструменты (действует для всех шагов):
- Инструменты — переменные окружения с описанием, значения по умолчанию — `~/opt/`:
  - `MAT_JDK_HOME` — JDK 21: для Eclipse MAT 1.17 и для запуска скриптов кейса
    из подпапки `scripts/`. Дефолт: `$HOME/opt/jdk-21.0.13+9-temurin`.
  - `MAT_HOME` — Eclipse MAT 1.17: GUI-анализ дампов (шаг 4) и classpath для
    скриптов кейса (все plugin-джары из `$MAT_HOME/plugins/`).
    Дефолт: `$HOME/opt/mat-1.17`.
  - MAT 1.17 требует JDK 21; системная старая Java не подходит.
- Правило приобретения инструментов: использовать из `~/opt/` → если инструмента
  нет — спросить владельца, где его взять → если владелец не указывает — спросить,
  можно ли скачать самостоятельно → скачивать — только с его слова.
- Конфигурация деплоя (JVM-команда, монтировки логов и дампов, БД) — всегда АКТУАЛЬНАЯ,
  берётся от боевого деплоя при каждой работе с кейсом, с provenance (путь + sha256);
  хардкодить её в кейсе нельзя (конфигурация меняется со временем).
- Рабочие файлы разбора (выдержки логов, дампы, отчёты) — на время работы в
  `.scratch/<проект>/work/<название>/`; в сценарий и в кейс не пишутся.

Шаг 0. Обязательная стадия проверки ссылок (не рекомендация).
- Что проверяем: актуальность всех кодовых ссылок сценария.
- Чем: поиск каждой строки в коде (репозиторий) и в sources-джарах библиотек
  (hazelcast-all-3.6.2-sources.jar, джар метрик из сборки taskurotta-*.jar — javap).
- Ожидаемый результат: все строки и классы найдены — кейс валиден.
- Если другой: не гадать — открыть файл/класс, установить, куда переехала строка или
  что поменялось; дальше по регламенту (устаревший кейс — по ситуации / по слову владельца).

Шаг 1. Логи: есть ли сигнатуры памяти/GC и какое окно.
- Что проверяем: (a) OOME; (b) STW-паузы (разрывы heartbeat Hazelcast — десятки секунд);
  (c) медленные операции Hazelcast; (d) операции, не стартующие на ноде; (e) ошибки
  REST-слоя; (f) деградация латентности и 5xx по request-логу.
- Чем: поиск по приложенному логу (bind-монт на хосте — путь из актуальной конфигурации
  деплоя), request-логу и docker json-log контейнера (stdout/stderr). ВНИМАНИЕ (грабли):
  текст OOME живёт в docker json-log, а не в приложенном логе: редирект `&>` в command
  compose-образа docker'ом разбивается на argv и не применяется (консольный лог-файл
  не создаётся), поэтому `console.log`/`stdout-лог` — только как строки json-log.
  Команды:
  ```
  docker logs <контейнер> 2>&1 | grep -n 'OutOfMemoryError' | head -50
  grep -n 'System clock apparently jumped' service.log | head -50
  grep -n 'Slow operation detected' service.log | head -50
  grep -n "'is-executing': false" service.log | head -50
  grep -n 'Aborting invocation' service.log | head -50
  awk '$NF+0>m{m=$NF+0} END{print "max_response_ms="m}' request.log
  awk '$9 ~ /^5/' request.log | head -50
  ```
- Ожидаемый результат: хотя бы одна из сигнатур: OOME; heartbeat-разрывы 10–40 с;
  `'is-executing': false`; латентность, растущая от сотен мс до минут.
- Если другой: сигнатур памяти/GC нет — не наш тип (развилка: общий разбор — CPU/сеть/БД).
  Паузы есть, но `'is-executing'` и SOED отсутствуют — проверить зомби-состояние
  сразу по шагу 3 (локи).
- Кодовые ссылки:
  - `код: hazelcast-all-3.6.2-sources.jar → com.hazelcast.cluster.impl.ClusterHeartbeatManager → "System clock apparently jumped from %s to %s since last heartbeat (%+d ms)"`
  - `код: hazelcast-all-3.6.2-sources.jar → com.hazelcast.spi.impl.operationexecutor.slowoperationdetector.SlowOperationDetector → "Slow operation detected: %s (%d invocations)"`
  - `код: hazelcast-all-3.6.2-sources.jar → com.hazelcast.spi.impl.operationservice.impl.IsStillRunningService → "'is-executing': " + executing + " -> " + invocation`
  - `код: hazelcast-all-3.6.2-sources.jar → com.hazelcast.spi.impl.operationservice.impl.Invocation → "return new OperationTimeoutException(\"No response for \" + totalTimeoutMs + \" ms.\""`
  - `код: dropwizard/src/main/java/ru/taskurotta/dropwizard/resources/TaskPollerResource.java → TaskPollerResource → "logError(\"Poll task failed!\", e);"`
  - `код: taskurotta/src/main/resources/hz-pg-mongo.yml → (server.requestLog) → "currentLogFilename: \"./target/logs/request.log\""`

Шаг 2. Хеп-дамп: откуда взять.
- Что проверяем: наличие и путь хеп-дампа OOME (флаг `-XX:+HeapDumpOnOutOfMemoryError`
  в JVM-команде) либо возможность снять его с живого/зомби-инстанса.
- Чем: (a) OOME-дамп: взять файл из контейнера — `docker cp <контейнер>:/<HeapDumpPath>/<файл>.hprof <локально>`
  (или из bind-монта на хосте); (b) живой/зомби-инстанс:
  `docker exec <контейнер> jmap -dump:live,format=b,file=<HeapDumpPath>/<имя>.hprof 1`
  (PID 1 — JVM; `jmap` присутствует в образе). ВНИМАНИЕ (грабли): хеп-дамп, записанный
  в именованный том, теряется при пересоздании контейнера — стойкая мера: bind-mount
  директории дампов на хост; тип монтировки проверить по актуальному инспекту/конфигу
  контейнера. Размер: полный 1024 МБ хеп — дамп ~1.1–1.5 ГБ (HPROF-обвязка), хеп на
  60% — ~0.7–0.9 ГБ.
- Ожидаемый результат: дамп-файл ожидаемого размера на диске.
- Если другой: дампа нет, а инстанс — зомби (JVM жив, сервис не отвечает) — сначала
  дамп потоков (шаг 3); jmap зависает — повторить без `:live`; дамп уже был снят при
  OOME, но контейнер пересоздан до сбора — повторить инцидент/воспроизвести (шаг 6).
- Кодовые ссылки:
  - `код: docker-compose/docker-compose_pg.yml → (command) → "java -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/javadumps"`
  - `код: util/src/main/java/ru/taskurotta/util/MemoryAllocationConfigurator.java → MemoryAllocationConfigurator → "logger.info(\"Total memory: {}Mb\", bytesToMb(totalMemory));"`

Шаг 3. Дамп потоков: зомби-состояние, владельцы локов.
- Что проверяем: состояние потоков JVM: зомби (владельцы локов погибли) vs работа vs
  истощение пулов; группы потоков и на каких локах стоят; turnover (номера потоков).
- Чем: `docker exec <контейнер> jstack 1 > thdump-<метка>.txt` (или `kill -3` →
  docker json-log). Анализ: группировка по именам групп; поиск «parking to wait for
  <адрес>» на одном адресе у сотен потоков при отсутствии треда в critical section;
  «Found N deadlock(s)»; номера потоков (turnover).
- Ожидаемый результат (зомби): группы потоков WAITING на одном лок-адресе без владельца:
  (a) сотни Jetty-потоков на `ReentrantReadWriteLock$ReadLock.lock` →
  `ExponentiallyDecayingReservoir.lockForRegularUsage` (писатель reservoir'а погиб);
  (b) десятки потоков HZ на `ReentrantLock` в
  `InternalPartitionServiceImpl.publishPartitionRuntimeState`, partition-треды — на
  ПУСТЫХ очередях. Круговых deadlocks нет (`waiting to lock` = 0).
- Если другой: потоки работают (JDBC-кадры, HZ-операции, обработка запросов) — зомби
  нет, продолжить со шага 4; всё parked, но у каждого лока есть владелец — не наш тип
  (иная причина: голод CPU, сеть — развилка).
- Кодовые ссылки:
  - `код: services-hz/src/main/java/ru/taskurotta/service/hz/gc/HzGarbageCollectorService.java → HzGarbageCollectorService → "thread.setName(\"GC-\" + counter++);"`
  - `код: services/src/main/java/ru/taskurotta/service/metrics/MetricsFactory.java → MetricsFactory → "thread.setName(\"Metrics-job-\" + counter++);"`
  - `код: hazelcast-all-3.6.2-sources.jar → com.hazelcast.partition.impl.InternalPartitionServiceImpl → "private void publishPartitionRuntimeState() {"`
  - `код: com/codahale/metrics/ExponentiallyDecayingReservoir.class (в сборке taskurotta-*.jar; проверить javap по собранному джару) → com.codahale.metrics.ExponentiallyDecayingReservoir → "private void lockForRegularUsage();"`

Шаг 4. MAT: кто занимает хеп.
- Что проверяем: распределение живого хепа: метрики vs HZ-мапы vs бизнес/остальное;
  доля метрик в живом хепе.
- Чем: Eclipse MAT 1.17 из `$MAT_HOME` (JDK 21 из `$MAT_JDK_HOME`): Overview,
  Leak Suspects, dominator tree, class histogram (классы: `DataPointVO`, `DataRowVO`,
  `NumberDataRowVO`, `Double`, `CachedDataRecordWithStats`, `CHM$Node`).
- Ожидаемый результат (сигнатура plateau метрик): retained
  `MetricsDataHandler` + `NumberDataHandler` ≥ 50% живого хепа; миллионы объектов
  `DataPointVO`.
- Если другой: топ — `MapServiceContextImpl` (HZ-мапы под кэпом) или бизнес-объекты —
  соответствующая ветка дерева сигнатур (раздел ниже).
- Кодовые ссылки:
  - `код: services/src/main/java/ru/taskurotta/service/metrics/handler/MetricsDataHandler.java → MetricsDataHandler → "private Map<String, DataRowVO> lastHourDataHolder = new ConcurrentHashMap<>();"`
  - `код: services/src/main/java/ru/taskurotta/service/metrics/handler/NumberDataHandler.java → NumberDataHandler → "private Map<String, NumberDataRowVO> dataHolder = new ConcurrentHashMap<>();"`
  - `код: taskurotta/src/main/resources/spring/backend/hazelcast.xml → (bean) → "<bean id=\"metricsDataHandler\" class=\"ru.taskurotta.service.metrics.handler.MetricsDataHandler\" init-method=\"init\"/>"`
  - `код: taskurotta/src/main/resources/default.properties → (hz.map) → "hz.map.task.name=Task"`

Шаг 5. Срез метрик-подсистемы (MetricsCurve) + остаток (HeapProbe).
- Что проверяем: точные цифры метрик: retained хэндлеров, число строк в holders,
  число ключей, заполненность колец, n/shallow `DataPointVO`; для «остатка» —
  HZ-мапы (записи, retained) и бизнес.
- Чем: скрипты из подпапки кейса: `scripts/metrics-curve.sh <дамп.hprof>` и
  `scripts/heap-probe.sh <дамп.hprof>` (запуск: JDK 21 из `$MAT_JDK_HOME`, classpath —
  все plugin-джары MAT 1.17 из `$MAT_HOME/plugins/`).
- Ожидаемый результат: retained хэндлеров совпадает с долей из шага 4; ключи =
  n(CheckPoint); `DataPointVO` = содержимое колец; теоретический максимум колец —
  (методические ключи × 2 × 3600) + (методические ключи × 2 × 1440) +
  (ключи queueSize × 17280) + (ключи memory × 8640); для keyspace первого инцидента —
  1611×3600×2 + 1611×1440×2 + 366×17280 + 3×8640 = 22.59 М слотов (фактически
  заполнено 82.5% — часть строк моложе времени жизни инстанса).
- Если другой (метрики малы): аттрибуция остатка по HeapProbe (HZ-мапы, сериализованные
  записи, бизнес) — ветки дерева сигнатур.
- Кодовые ссылки:
  - `код: services/src/main/java/ru/taskurotta/service/metrics/Metric.java → Metric → "private final ConcurrentMap<String, CheckPoint> dataSets = new ConcurrentHashMap<>();"`
  - `код: services/src/main/java/ru/taskurotta/service/metrics/CheckPoint.java → CheckPoint → "State state = stateRef.getAndSet(new State());"`
  - `код: services/src/main/java/ru/taskurotta/service/metrics/model/DataRowVO.java → DataRowVO → "this.dsCounts.set(position, new DataPointVO<Long>(count, measureTime));"`
  - `код: services/src/main/java/ru/taskurotta/service/metrics/model/BaseDataRowVO.java → BaseDataRowVO → "protected volatile long updated = -1L;"`
  - `код: docs/support/1-taskurotta-memory-oom/scripts/metrics-curve.sh → (запуск) → "MetricsCurve"`

Шаг 6. Если нужно воспроизвести.
- Что проверяем: воспроизводится ли механизм на актуальной сборке и актуальной
  конфигурации деплоя (окружение «как на сервере»), с серией дампов и единой
  методикой атрибуции.
- Чем: плейбук `docs/howto/e2e-regression-run/` (окружение, профиль нагрузки, стратегия
  замеров). Обвязка прогона в подпапку кейса НЕ копируется (без дублирования знаний) —
  только ссылка.
- Ожидаемый результат: память метрик растёт по wall-clock процесса (не по нагрузке),
  сигнатура совпадает с инцидентом; plateau = (число ключей) × (заполненность колец).
- Если другой: механизм изменился (фикс в сборке / иная причина) — «анализ анализа»
  по регламенту: новый кейс / дополнение этого / не кейс.
- Кодовая ссылка:
  - `код: docs/howto/e2e-regression-run/playbook.md → (плейбук) → "# How-to: Регрессионный E2E-прогон (e2e-regression-run)"`

Шаг 7. Вывод (отчёт).
- Что проверяем: итог по инциденту: что заполнило хеп (доли %), сколько ключей метрик
  и насколько заполнены кольца, триггер (нагрузка: окно массового старта задач по
  request-логу), окно OOME, зомби-состояние (число ожидателей, какие локи), меры
  (bind-mount дампов, фикс подсистемы, превентивный мониторинг).
- Чем: сопоставление цифр шагов 4–5 с таймлайном шага 1 и дампом потоков шага 3;
  калибровка старта инстанса — строка автокалибровки памяти в стартовом логе
  («Automatic memory calibration» / «Total memory … Mb»).
- Ожидаемый результат: вывод вида «plateau метрик X МБ (Y% живого хепа), ключи — Z,
  триггер — <нагрузка/окно>, HZ-мапы — W МБ (не причина), зомби — лока без владельца
  (N ожидателей)» + рекомендации.
- Если другой (механизм не установлен): открыть задачу в трекере (по регламенту §8),
  в «Корневая причина» — «не найдена: <что проверить, у кого спросить>».
- Кодовая ссылка:
  - `код: util/src/main/java/ru/taskurotta/util/MemoryAllocationConfigurator.java → MemoryAllocationConfigurator → "logger.info(\"Automatic memory calibration: \");"`

### Дерево сигнатур

1. **Plateau метрик** (метрики ≥ 50% живого хепа) — первый инцидент.
   - Как распознать в дампе: retained `MetricsDataHandler` (хранилища
     `lastHourDataHolder` / `lastDayDataHolder`) + `NumberDataHandler` (`dataHolder`)
     ≥ 50% живого хепа; `DataPointVO` — миллионы объектов; ключи — строки вида
     «метрика#датасет» (метрики — имена перечисления `MetricName`; датасеты —
     FQN+версия актёра/джоба, плюс агрегатные ключи, где датасет = имя метрики).
   - Что значит: ключи создаются при первом обращении и НИГДЕ не удаляются (нет
     remove/evict строк); кольца (час — 3600 слотов, день — 1440, number — 86400/период)
     заполняются планировочным потоком дампинга со ВРЕМЕНЕМ ПРОЦЕССА (wall-clock),
     независимо от активности: дампинг обходит ВСЕ ключи и пишет точку даже для ключа
     с count=0. Plateau = (число ключей) × (размеры колец); ограничен класс-базовым
     keyspace'ом актёров/джоб.
   - Куда копать: MetricsCurve (шаг 5) — n(CheckPoint), строки holders, заполненность
     колец; поля `BaseDataRowVO.updated` / `lastActive` — основа для TTL/чистки
     неактивных ключей; источники меток — `Metric.mark` (кто и с какими датасетами
     метит).
2. **HZ-мапы** (Task / TaskDecision / Graph / Process).
   - Как распознать: retained `MapServiceContextImpl` (все мапы HZ); по мапам — записи
     (обход records) и собственный учёт heapCost; cap мапы — 7% хепа (свойство
     `hz.map.*.memory.max-size`, политика USED_HEAP_PERCENTAGE), eviction LRU 25% в
     MapStore (writeDelay=0 → каждая запись уходит в БД).
   - Что значит: мапы закапаны — не безлимитный источник; если мапа у кэпа — LRU
     вытесняет активные данные в БД (латентность под нагрузкой); бэклог завершённых
     процессов (срок удержания `gc.process.finished.wait-before-delete`, по боевому
     профилю — 1 сутки) сидит в Task/TaskDecision/Graph и конкурирует за тот же кэп.
   - Куда копать: число записей по каждой мапе и доля завершённых (не активных)
     процессов; бэклог внутренних очередей (очередь решений); время удержания
     завершённых процессов в конфигурации.
3. **Пулы и потоки.**
   - Как распознать (дамп потоков): группы: `dw-*` (Jetty/dropwizard, maxThreads 2000),
     `cached*` (HZ cached executor), `hz partition-operation` / `hz generic-operation`
     (пустая очередь = простой), `GC-N` (внутренний GC taskurotta, `gc.pool-size`),
     `Metrics-job-N` (треды дампинга метрик), `metrics-meter-tick-thread-*`.
   - Что значит: тысячи/сотни потоков — НЕ истощение (пулы разрастаются штатно);
     «истощение пула» — отдельный тип инцидента: потоки ждут ПОЯВЛЕНИЯ свободных
     потоков пула (а не стоят на локе). Все parked на локах → зомби (сигнатура 4);
     все на пустых очередях → простой (JVM в зомби-полусне).
   - Куда копать: состояние каждой группы; владельцы локов; номера потоков (turnover).
4. **Зомби-состояние после OOME** (локи без владельца).
   - Как распознать: дамп потоков — N ожидателей («parking to wait for <адрес>») на
     ОДНОМ лок-адресе при нуле треда в critical section: (a) HZ partition state —
     40+ потоков (`cached*` + migration) на ReentrantLock в
     `InternalPartitionServiceImpl`, partition-треды простоявают на пустых очередях;
     (b) codahale-резервуар request-Timer — сотни Jetty-потоков на read-локе
     `ExponentiallyDecayingReservoir.lockForRegularUsage`, писателя
     (`lockForAggregation`/`getSnapshot`) в дампе нет. В hprof-дампе — AQS-объекты
     (waitQueue), НО адреса плывут с GC: лок, видимый в дампе потоков через N часов,
     в более раннем hprof уже может быть стёрт — разница времён дампов не опровергает
     механизм.
   - Что значит: владелец лока погиб внутри critical section (неотловленный OOME);
     ReentrantLock/ReadWriteLock не освобождаются автоматически; инстанс
     функционально мёртв (Jetty-пул заморожен, HZ не планирует операции) — единственный
     выход: рестарт; причина OOME — отдельно, по hprof (шаги 4–5).
   - Куда копать: кто держал лок (critical section в hprof), окно OOME (docker json-log),
     время hprof vs время дампа потоков.

## Корневая причина

Первый инцидент: метрик-подсистема. Ключи «метрика#датасет» создаются при первом
обращении актёра/джоба и никогда не удаляются (кода удаления/чистки строк нет);
кольца строк заполняются планировочным потоком дампинга со временем жизни процесса
(wall-clock), а не интенсивностью нагрузки — для неактивного ключа пишется точка
со значением 0. Plateau = (число ключей) × (размеры колец): на keyspace первого
инцидента — ~688 МБ (81% живого хепа) из бюджета 1024 МБ, что оставляет мало воздуха
под нагрузку. Триггер OOME — всплеск ~7400 задач поверх plateau: GC-спираль
(STW-паузы 13–43 с), затем OOME; после OOME — утечка двух локов без владельца
(partition state Hazelcast, write-лок request-Timer codahale-metrics): владельцы
погибли внутри critical section — перманентное зомби-состояние (~8 ч). HZ-мапы
причинами НЕ являются: каждая закапана на 7% хепа (LRU-eviction в MapStore), ни одна
не уперлась в кэп (суммарно ~88 МБ).

## Первый инцидент

Площадка — автотест-инфраструктура (контейнер taskurotta, образ 12.5, JDK 1.8.0_66,
Hazelcast 3.6.2 single-node, -Xmx1024m). Заявка в поддержку — не поступала: инцидент
обнаружен владельцем (фрагмент: «Сервис не отвечал»; полный исходный текст — в
материалах разбора).

Timeline (время МСК = UTC+3; времена в логах контейнера — UTC):
- 2026-10-07 19:24 — запуск инстанса (автокалибровка памяти в стартовом логе:
  Total memory 981.50 Mb, Free 70%: 653.45 Mb, kernel 48 Mb).
- 2026-11-05 19:11–21:00 — всплеск: массовый старт ~7400 задач (35× фонового уровня
  по request-логу), ответная волна poll(200)/release.
- 2026-11-05 22:07–22:21 — GC-спираль: STW-паузы 13–43 с (heartbeat-разрывы),
  `'is-executing': false`, бэклог in-flight HZ-вызовов 451→624; латентность non-poll
  500 мс → 26 с → 6,6 мин; первые 5xx — 22:18.
- 2026-11-05 22:21:45 — последняя строка приложенного лога (ERROR, PollOperation
  timeout).
- 2026-11-05 22:21–22:29 — окно OOME; 22:29:05 — записан хеп-дамп (HeapDumpOnOutOfMemoryError).
- 2026-11-05 22:29 → 2026-11-06 06:31 — зомби: ноль внешних запросов; в docker json-log
  172 строки OOME (`dw-*`, `metrics-meter-tick-thread-*` — потоки погибали от OOME ещё
  8+ часов после первого); локи без владельца: 41 ожидатель на partition state HZ,
  532 Jetty-потока на read-локе codahale-резервуара.
- 2026-11-06 06:27 — дамп потоков (808 потоков).
- 2026-11-06 06:31 — ручной stop/start; новый инстанс работает с 06:31:26.

Ключевые цифры (hprof, живой хеп 830.3 MiB = 81.1% от -Xmx1024m):
- Метрики: MetricsDataHandler 509.1 МБ + NumberDataHandler 178.6 МБ = **687.7 МБ
  (81.1% живого хепа)**; ключей «метрика#датасет» — 1611 (плюс 369 number-ключей:
  queueSize 366, memory 3); `DataPointVO` — 18 651 972 (shallow 447.6 МБ).
- HZ-мапы: Task ~34 МБ / TaskDecision ~32 МБ / Graph ~3 МБ (суммарно ~88 МБ), кэп
  71.7 МБ на мапу — ни одна не уперлась.
- Триггер: всплеск ~7400 задач (19:11–21:00 МСК).

Доказательная база (файлы получены от владельца; лежат на рабочей машине, в git не
попадают — фиксация по sha256 и описанию):
- хеп-дамп `java_pid1.hprof`, 1 396 900 848 B, sha256 `9324945c483d7c13b39bade7482008b7512729b4cbe7f30874bc0dbc902b57c2` (в пакете — `tmp/diag-20261106/`);
- дамп потоков `javathdump` (808 потоков), sha256 `0db3e659615ba29e879b0af6937b0b578d15101aab12c6716fab00353cd826c8`;
- хвост приложенного лога `service.log` (2077 строк), sha256 `8569c437206f8685c7a223f6125759cb711617fa3a4715926430408cb77c0eac`;
- исходный диагностический пакет `taskurotta-diag-20261106.tar.gz` (220 MiB).

## Журнал

2026-11-06 — Кейс создан со сценарием (состояние: READY). Сценарий построен на
разборе первого инцидента (OOME под нагрузкой): вердикт — plateau метрик-подсистемы
(687.7 МБ, 81.1% живого хепа) + триггер-нагрузка; зомби-состояние после OOME
(локи без владельца).

## Links

- [плейбук регрессионного E2E-прогона](../../howto/e2e-regression-run/playbook.md) — воспроизведение
  механизма (шаг 6 сценария).
