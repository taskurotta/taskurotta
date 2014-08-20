CREATE TABLE process (
  process_id    VARCHAR2(36)        NOT NULL,
  start_task_id VARCHAR2(36)        NOT NULL,
  custom_id     VARCHAR2(256)       NULL,
  start_time    NUMBER              NOT NULL,
  end_time      NUMBER              NULL,
  state         NUMBER              NOT NULL,
  return_value  CLOB                NULL,
  START_JSON    CLOB                NULL,
  actor_id       VARCHAR2(500 char),
  PRIMARY KEY (process_id)
)
NOCOMPRESS
;

exit;

alter table process add actor_id VARCHAR2(500 char);