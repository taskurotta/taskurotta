CREATE TABLE QB$QUEUE_NAMES
(
  QUEUE_ID   NUMBER        NOT NULL,
  QUEUE_NAME VARCHAR2(256) NOT NULL,
  PRIMARY KEY (QUEUE_ID)
);

CREATE UNIQUE INDEX QB$UNIQUE_NAME
ON QB$QUEUE_NAMES (QUEUE_NAME ASC);

CREATE SEQUENCE QB$SEQUENCE MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;
CREATE SEQUENCE QB$CHECKPOINT_SEQUENCE MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;


CREATE TABLE TASK (
  UUID        VARCHAR2(36) NOT NULL,
  PROCESS_ID  VARCHAR2(36) NOT NULL,
  JSON_VALUE  CLOB         NULL,
  PRIMARY KEY (UUID)
)
NOCOMPRESS
;
CREATE INDEX TASK_IDX_PROCESS ON TASK(PROCESS_ID);

CREATE TABLE DECISION (
  TASK_ID       VARCHAR2(36) NOT NULL,
  PROCESS_ID    VARCHAR2(36) NOT NULL,
  DECISION_JSON CLOB         NOT NULL,
  PRIMARY KEY (TASK_ID, PROCESS_ID)
)
NOCOMPRESS
;


CREATE TABLE TR_CHECKPOINTS (
  ID              NUMBER        NOT NULL,
  TASK_ID         VARCHAR2(36)  NOT NULL,
  PROCESS_ID      VARCHAR2(36)  NOT NULL,
  ACTOR_ID        VARCHAR2(255 char) NOT NULL,
  TYPE_TIMEOUT    VARCHAR2(36)  NOT NULL,
  CHECKPOINT_TIME NUMBER        NOT NULL,
  PRIMARY KEY (ID)
)
NOCOMPRESS
;

ALTER TABLE DECISION ADD CONSTRAINT TASK_DECISION FOREIGN KEY (TASK_ID) REFERENCES TASK (UUID);

CREATE TABLE GRAPH (
  ID       VARCHAR2(36) NOT NULL,
  VERSION  NUMBER       NOT NULL,
  JSON_STR CLOB         NOT NULL,
  PRIMARY KEY (id)
)
NOCOMPRESS
;

CREATE TABLE GRAPH_DECISION (
  FINISHED_TASK_ID  VARCHAR2(36) NOT NULL,
  READY_ITEMS       CLOB         NOT NULL,
  MODIFICATION_JSON CLOB         NOT NULL,
  PRIMARY KEY (FINISHED_TASK_ID)
)
NOCOMPRESS
;

CREATE OR REPLACE TRIGGER TR_CHECKPOINTS_BI
BEFORE INSERT
ON TR_CHECKPOINTS
FOR EACH ROW
  BEGIN
    IF :NEW.id IS NULL
    THEN
      SELECT
        QB$CHECKPOINT_SEQUENCE.NEXTVAL
      INTO :NEW.id
      FROM DUAL;
    END IF;
  END;
/

CREATE TABLE process (
  process_id    VARCHAR2(36)  NOT NULL,
  start_task_id VARCHAR2(36)  NOT NULL,
  custom_id     VARCHAR2(256) NULL,
  start_time    NUMBER        NOT NULL,
  end_time      NUMBER        NULL,
  state         NUMBER        NOT NULL,
  return_value  CLOB          NULL,
  PRIMARY KEY (process_id)
)
NOCOMPRESS
;

ALTER TABLE process ADD CONSTRAINT custom_id_uq UNIQUE (custom_id);

DELETE FROM DECISION;
DELETE FROM TASK;

ALTER TABLE TASK
ADD ( NUMBER_OF_ATTEMPTS NUMBER NOT NULL  )
ADD ( ACTOR_ID VARCHAR2(256) NOT NULL  );

ALTER TABLE DECISION
ADD ( IS_ERROR NUMBER(1) NOT NULL  )
ADD ( DECISION_DATE NUMBER NOT NULL  );

CREATE OR REPLACE
PROCEDURE ADD_DECISION(
  P_TASK_ID       IN VARCHAR2,
  P_PROCESS_ID    IN VARCHAR2,
  P_DECISION_JSON IN CLOB,
  P_IS_ERROR      IN NUMBER,
  P_DECISION_DATE IN NUMBER)
AS
  BEGIN
    UPDATE DECISION
    SET DECISION_JSON = P_DECISION_JSON,
      IS_ERROR        = P_IS_ERROR,
      DECISION_DATE   = P_DECISION_DATE
    WHERE task_id = P_TASK_ID;
    IF (sql%rowcount = 0)
    THEN
      INSERT
      INTO DECISION
      (
        TASK_ID,
        PROCESS_ID,
        DECISION_JSON,
        IS_ERROR,
        DECISION_DATE
      )
        VALUES
        (
          P_TASK_ID,
          P_PROCESS_ID,
          P_DECISION_JSON,
          P_IS_ERROR,
          P_DECISION_DATE
        );
    END IF;
  END;
/

CREATE OR REPLACE
PROCEDURE add_graph_decision(P_FINISHED_TASK_ID IN VARCHAR2, P_READY_ITEMS IN CLOB, P_MODIFICATION_JSON IN CLOB)
AS
  BEGIN
    UPDATE GRAPH_DECISION D
    SET D.READY_ITEMS = P_READY_ITEMS,
      D.MODIFICATION_JSON= P_MODIFICATION_JSON
    WHERE FINISHED_TASK_ID = P_FINISHED_TASK_ID;
    IF (sql%rowcount = 0)
    THEN
      INSERT INTO GRAPH_DECISION (FINISHED_TASK_ID, READY_ITEMS, MODIFICATION_JSON)
        VALUES (P_FINISHED_TASK_ID, P_READY_ITEMS, P_MODIFICATION_JSON);

    END IF;

  END;
/

ALTER TABLE TASK
ADD ( START_TIME TIMESTAMP NULL  );

ALTER TABLE QB$QUEUE_NAMES
ADD ( QUEUE_TABLE_NAME VARCHAR2(256) NULL);


ALTER TABLE PROCESS
ADD ( START_JSON CLOB NULL  ) ;

exit;
