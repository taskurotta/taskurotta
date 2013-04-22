package ru.taskurotta.backend.checkpoint;

/**
 * Type of the monitored timeout
 */
public enum TimeoutType {

    //PROCESS BACKEND (timeouts managed by process backend):
    //from startProcessCommit()[ends] to finishProcess()[ends]
    PROCESS_START_TO_CLOSE ("PROCESS_START_TO_CLOSE"),
    //do we really need it? It duplicates TASK_SCHEDULE_TO_START for the process first start task
    PROCESS_SCHEDULE_TO_START ("PROCESS_SCHEDULE_TO_START"),
    //from startProcess()[begin] to finishProcess()[ends]
    PROCESS_SCHEDULE_TO_CLOSE ("PROCESS_SCHEDULE_TO_CLOSE"),
    //from startProcess()[begin] to startProcessCommit()[ends]
    PROCESS_START_TO_COMMIT("PROCESS_START_TO_COMMIT"),

    //TASK BACKEND (timeouts managed by task backend):
    //from getTaskToExecute()[ends] to addDecisionCommit()[ends]
    TASK_START_TO_CLOSE ("TASK_START_TO_CLOSE"),
    //from addDecision[begin] to addDecisionCommit[ends]
    TASK_RELEASE_TO_COMMIT("TASK_RELEASE_TO_COMMIT"),

    //QUEUE BACKEND (timeouts managed by queue backend):
    //from enqueueItem()[on task.startTime] to pollCommit()[ends]
    TASK_SCHEDULE_TO_START("TASK_SCHEDULE_TO_START"),
    //from poll()[on obtaining task] to pollCommit()[ends]
    TASK_POLL_TO_COMMIT("TASK_POLL_TO_COMMIT"),

    //CROSS-BACKENDS (timeouts added and removed in different backends):
    //TODO: Require some workaround, cause works only if backends reusing same checkpoints storage

    //queue: enqueueItem()[on task.startTime] to task: addDecisionCommit()[ends]
    TASK_SCHEDULE_TO_CLOSE ("TASK_SCHEDULE_TO_CLOSE");


    private String value;

    private TimeoutType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value!=null? this.value: super.toString();
    }

    public static TimeoutType forValue(String value) {
        if("PROCESS_START_TO_CLOSE".equalsIgnoreCase(value)) {
            return TimeoutType.PROCESS_START_TO_CLOSE;
        } else if("PROCESS_SCHEDULE_TO_START".equalsIgnoreCase(value)) {
            return TimeoutType.PROCESS_SCHEDULE_TO_START;
        } else if("PROCESS_SCHEDULE_TO_CLOSE".equalsIgnoreCase(value)) {
            return TimeoutType.PROCESS_SCHEDULE_TO_CLOSE;
        } else if("TASK_START_TO_CLOSE".equalsIgnoreCase(value)) {
            return TimeoutType.TASK_START_TO_CLOSE;
        } else if("TASK_SCHEDULE_TO_START".equalsIgnoreCase(value)) {
            return TimeoutType.TASK_SCHEDULE_TO_START;
        } else if("TASK_SCHEDULE_TO_CLOSE".equalsIgnoreCase(value)) {
            return TimeoutType.TASK_SCHEDULE_TO_CLOSE;
        } else if("TASK_POLL_TO_COMMIT".equalsIgnoreCase(value)) {
            return TimeoutType.TASK_POLL_TO_COMMIT;
        } else if("TASK_RELEASE_TO_COMMIT".equalsIgnoreCase(value)) {
            return TimeoutType.TASK_RELEASE_TO_COMMIT;
        } else if("PROCESS_START_TO_COMMIT".equalsIgnoreCase(value)) {
            return TimeoutType.PROCESS_START_TO_COMMIT;
        } else {
            throw new IllegalArgumentException("Cannot create TimeoutType for value["+value+"]");
        }
    }

}
