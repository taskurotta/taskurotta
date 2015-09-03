package ru.taskurotta.service.schedule;

/**
 * Date: 24.09.13 18:26
 */
public interface JobConstants {

    int STATUS_ACTIVE = 1;
    int STATUS_INACTIVE = -1;
    int STATUS_UNDEFINED = 0;
    int STATUS_ERROR = -2;

    String DATA_KEY_JOB = "job";
    String DATA_KEY_JOB_MANAGER = "jobManager";
    String DATA_KEY_TASK_SERVER = "taskServer";
//    String DATA_KEY_QUEUE_INFO_RETRIEVER = "queueInfoRetriever";
    String DATA_KEY_PROCESS_INFO_RETRIEVER = "processInfoRetriever";

    int DEFAULT_MAX_CONSEQUENTIAL_ERRORS = 3;
    int DEFAULT_NUMBER_OF_ATTEMPTS = 5;


}
