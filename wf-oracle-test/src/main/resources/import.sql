CREATE TABLE QUEUE_BUS
(
  TASK_ID       NUMBER                       NOT NULL,
  STATUS_ID     NUMBER                       NOT NULL,
  TYPE_ID       NUMBER                       NOT NULL,
  DATA_UPDATE   DATE,
  ACTOR_ID      VARCHAR(100)
)