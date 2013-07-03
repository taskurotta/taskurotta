package ru.taskurotta.backend.checkpoint;

import java.util.HashMap;
import java.util.Map;

/**
 * Type of the monitored timeout
 */
public enum TimeoutType {

    //PROCESS BACKEND (timeouts managed by process backend):
    //from startProcessCommit()[ends] to finishProcess()[ends]
    PROCESS_START_TO_CLOSE("PROCESS_START_TO_CLOSE"),
    //do we really need it? It duplicates TASK_SCHEDULE_TO_START for the process first start task
    PROCESS_SCHEDULE_TO_START("PROCESS_SCHEDULE_TO_START"),
    //from startProcess()[begin] to finishProcess()[ends]
    PROCESS_SCHEDULE_TO_CLOSE("PROCESS_SCHEDULE_TO_CLOSE"),
    //from startProcess()[begin] to startProcessCommit()[ends]
    PROCESS_START_TO_COMMIT("PROCESS_START_TO_COMMIT"),

    //TASK BACKEND (timeouts managed by task backend):
    //from getTaskToExecute()[ends] to addDecisionCommit()[ends]
    TASK_START_TO_CLOSE("TASK_START_TO_CLOSE"),
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
    TASK_SCHEDULE_TO_CLOSE("TASK_SCHEDULE_TO_CLOSE");


    private static final Map<String, TimeoutType> strToType = new HashMap<>(10);

    static {
        for (TimeoutType type : TimeoutType.values()) {
            strToType.put(type.value, type);
        }
    }

    private String value;

    private TimeoutType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value != null ? this.value : super.toString();
    }

    public static TimeoutType forValue(String value) {
        TimeoutType result = strToType.get(value);
        if (null == result) {
            throw new IllegalArgumentException("Can't create TimeoutType for value[" + value + "]");
        }
        return result;
    }

}
