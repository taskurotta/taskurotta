ALTER TABLE PROCESS RENAME TO TSK_PROCESS;

CREATE INDEX TSK_PROCESS_STATE_IDX ON TSK_PROCESS(STATE);
CREATE INDEX TSK_PROCESS_CUSTOM_ID_IDX ON TSK_PROCESS(CUSTOM_ID);

CREATE TABLE TSK_INTERRUPTED_TASKS (
  ID                NUMBER NOT NULL PRIMARY KEY,
  PROCESS_ID        VARCHAR2(50 char) NOT NULL,
  TASK_ID           VARCHAR2(50 char) NOT NULL,
  STARTER_ID        VARCHAR2(500 char) NOT NULL,
  ACTOR_ID          VARCHAR2(500 char) NOT NULL,
  CREATION_DATE     TIMESTAMP(6) NOT NULL,
  TIME              NUMBER NOT NULL,
  ERROR_MESSAGE     VARCHAR2(500 char),
  ERROR_CLASS_NAME  VARCHAR2(500 char),
  STACK_TRACE       CLOB
);

COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.PROCESS_ID IS 'Interrupted process GUID';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.TASK_ID IS 'Interrupted task GUID';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.STARTER_ID IS 'Actor ID of process starter actor';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.ACTOR_ID IS 'Actor ID of interrupted task';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.CREATION_DATE IS 'Row creation date';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.TIME IS 'Long representation of a failing time';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.ERROR_MESSAGE IS 'Message of the occured actor exception';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.ERROR_CLASS_NAME IS 'Full class name for the actor exception';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.STACK_TRACE IS 'Full stack trace of the exception got from actor';

CREATE SEQUENCE SEQ_TSK_INTERRUPTED_TASKS MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;

CREATE OR REPLACE TRIGGER TSK_INTERRUPTED_TASKS_BI
BEFORE INSERT
ON TSK_INTERRUPTED_TASKS
FOR EACH ROW
  BEGIN
    IF :NEW.id IS NULL
    THEN
      SELECT
        SEQ_TSK_INTERRUPTED_TASKS.NEXTVAL
      INTO :NEW.id
      FROM DUAL;
    END IF;
  END;
/

CREATE INDEX TSK_INTERRUPTED_TASKS_IP ON TSK_INTERRUPTED_TASKS(STARTER_ID);
CREATE INDEX TSK_INTERRUPTED_TASKS_IA ON TSK_INTERRUPTED_TASKS(ACTOR_ID);
CREATE INDEX TSK_INTERRUPTED_TASKS_IE ON TSK_INTERRUPTED_TASKS(ERROR_CLASS_NAME);
CREATE INDEX TSK_INTERRUPTED_TASKS_ITIME ON TSK_INTERRUPTED_TASKS(TIME);

DROP TABLE TSK_BROKEN_PROCESSES;
DROP SEQUENCE SEQ_TSK_BROKEN_PROCESSES;

exit;