CREATE TABLE TSK_PROCESS (
  process_id    VARCHAR2(36)        NOT NULL,
  start_task_id VARCHAR2(36)        NOT NULL,
  custom_id     VARCHAR2(256)       NULL,
  start_time    NUMBER              NOT NULL,
  end_time      NUMBER              NULL,
  state         NUMBER              NOT NULL,
  return_value  CLOB                NULL,
  START_JSON    CLOB                NULL,
  actor_id      VARCHAR2(500 char)  NOT NULL,
  PRIMARY KEY (process_id)
)
NOCOMPRESS;
-- /TSK_PROCESS

CREATE TABLE TSK_SCHEDULED
(
  ID                    NUMBER                    NOT NULL,
  NAME                  VARCHAR2(256 char)        NOT NULL,
  CRON                  VARCHAR2(256 char)        NOT NULL,
  STATUS                NUMBER                    NOT NULL,
  JSON                  VARCHAR2(2000 char)       NOT NULL,
  CREATED               TIMESTAMP,
  QUEUE_LIMIT           NUMBER,
  MAX_ERRORS            NUMBER,
  ERR_COUNT             NUMBER,
  LAST_ERR_MESSAGE      VARCHAR2(2000 char),
  PRIMARY KEY (ID)
);

CREATE SEQUENCE SEQ_TSK_SCHEDULED MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;

CREATE OR REPLACE TRIGGER TSK_SCHEDULED_BI
BEFORE INSERT
ON TSK_SCHEDULED
FOR EACH ROW
  BEGIN
    IF :NEW.id IS NULL
    THEN
      SELECT
        SEQ_TSK_SCHEDULED.NEXTVAL
      INTO :NEW.id
      FROM DUAL;
    END IF;
  END;
/

CREATE TABLE TSK_BROKEN_PROCESSES (
    ID                NUMBER NOT NULL PRIMARY KEY,
    PROCESS_ID        VARCHAR2(50 char) NOT NULL,
    START_ACTOR_ID    VARCHAR2(500 char) NOT NULL,
    BROKEN_ACTOR_ID   VARCHAR2(500 char) NOT NULL,
    CREATION_DATE     TIMESTAMP(6) NOT NULL,
    TIME              NUMBER NOT NULL,
    ERROR_MESSAGE     VARCHAR2(500 char),
    ERROR_CLASS_NAME  VARCHAR2(500 char),
    STACK_TRACE       CLOB
);

COMMENT ON COLUMN TSK_BROKEN_PROCESSES.PROCESS_ID IS 'Broken process GUID';
COMMENT ON COLUMN TSK_BROKEN_PROCESSES.START_ACTOR_ID IS 'Actor ID of process starter actor';
COMMENT ON COLUMN TSK_BROKEN_PROCESSES.BROKEN_ACTOR_ID IS 'Actor ID of process stop causing actor';
COMMENT ON COLUMN TSK_BROKEN_PROCESSES.CREATION_DATE IS 'Row creation date';
COMMENT ON COLUMN TSK_BROKEN_PROCESSES.TIME IS 'Long representation of a failing time';
COMMENT ON COLUMN TSK_BROKEN_PROCESSES.ERROR_MESSAGE IS 'Message of the occured actor exception';
COMMENT ON COLUMN TSK_BROKEN_PROCESSES.ERROR_CLASS_NAME IS 'Full class name for the actor exception';
COMMENT ON COLUMN TSK_BROKEN_PROCESSES.STACK_TRACE IS 'Full stack trace of the exception got from actor';

CREATE SEQUENCE  SEQ_TSK_BROKEN_PROCESSES MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;

CREATE OR REPLACE TRIGGER TSK_BROKEN_PROCESSES_BI
BEFORE INSERT
ON TSK_BROKEN_PROCESSES
FOR EACH ROW
  BEGIN
    IF :NEW.id IS NULL
    THEN
      SELECT
        SEQ_TSK_BROKEN_PROCESSES.NEXTVAL
      INTO :NEW.id
      FROM DUAL;
    END IF;
  END;
/

CREATE INDEX TSK_BROKEN_PROCESSES_IP ON TSK_BROKEN_PROCESSES(START_ACTOR_ID);
CREATE INDEX TSK_BROKEN_PROCESSES_IA ON TSK_BROKEN_PROCESSES(BROKEN_ACTOR_ID);
CREATE INDEX TSK_BROKEN_PROCESSES_IE ON TSK_BROKEN_PROCESSES(ERROR_CLASS_NAME);

CREATE INDEX TSK_BROKEN_PROCESSES_ITIME ON TSK_BROKEN_PROCESSES(TIME);

exit;
