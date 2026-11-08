# L2: Containers — технический состав taskurotta

Система собирается Maven'ом из корневых модулей (список из корневого `pom.xml`). Зависимости между модулями проверены по `pom.xml` каждого модуля.

## Слой фреймворка
| Модуль | Зависит от (внутр.) | Роль |
| --- | --- | --- |
| `core` | — (только logback-core) | Ядро фреймворка, база для остальных слоёв. |
| `transport` | `core` | Транспортный слой: перенос сообщений между узлами (jackson для сериализации). |
| `server` | `transport` | Серверная часть: приёма и обработки запросов на ноде. |
| `client` | `core`, `server` | Клиентская часть (jersey-apache-client) — доступ к системе из приложений. |
| `util` | — | Утилитарная библиотека (в pom-описании: «must depend only from Java»). |

## Слой хранения
| Модуль | Зависит от (внутр.) | Роль |
| --- | --- | --- |
| `services` | `core`, `server`, `transport`, `util` | Базовые сервисы хранения, общие для всех бэкендов. |
| `mongodb` | — (hazelcast-all, mongo-driver, spring) | Интеграция с MongoDB. |
| `hazelcast` | `mongodb`, `util` | Интеграция с Hazelcast (распределённое состояние). |
| `services-hz` | `services`, `hazelcast`, `server`, `mongodb` | Сервисы хранения поверх Hazelcast. |
| `services-ora` | `services-hz` | Персистентность в Oracle (spring-jdbc/spring-tx). |
| `services-pg` | `services-hz` | Персистентность в PostgreSQL (spring-jdbc/spring-tx). |

## Слой запусков и сборки
| Модуль | Зависит от (внутр.) | Роль |
| --- | --- | --- |
| `dropwizard` | `services`, `util` | Сервер на Dropwizard; отдача веб-ассетов (bundle assets). |
| `spring` | `bootstrap` | Сервер на Spring. |
| `bootstrap` | `client`, `transport`, `util` | Сборка конфигурации/зависимостей для старта. |
| `taskurotta` | все перечисленные выше | Финальная сборка: собирает все слои в один артефакт (`taskurotta/target/taskurotta-$MVN_PROJECT_VERSION.jar`). |
| `recipes` | `dropwizard`, `hazelcast`, `services-hz`, `spring`, `test` | Готовые рецепты конфигураций. |
| `test` | `dropwizard`, `hazelcast`, `services`, `services-hz`, `spring` | Модуль интеграционных тестов (профиль `integration` в корневом pom). |
| `documentation` | — | Ассеты документации (отдаются веб-сервером `tsk_web_doc_dev`). |

## Вне Maven-модулей
- `autotest/` — автоматизированные тесты (скрипты `ff-test.bat`, `self.bat` в корне).
- `notifications/`, `job_store/`, `logs/`, `scripts/` — служебные каталоги репозитория, не модули сборки.
- `docker/`, `docker-compose/` — развёртывание; детали — [плейбук окружения](../howto/project-environment/playbook.md).
