package ru.taskurotta.backend.checkpoint;

/**
 * Type of the monitored timeout
 */
public enum TimeoutType {

    PROCESS_START_TO_CLOSE ("PROCESS_START_TO_CLOSE"),
    PROCESS_SCHEDULE_TO_START ("PROCESS_SCHEDULE_TO_START"),
    PROCESS_SCHEDULE_TO_CLOSE ("PROCESS_SCHEDULE_TO_CLOSE"),

    TASK_START_TO_CLOSE ("TASK_START_TO_CLOSE"),
    TASK_SCHEDULE_TO_START("TASK_SCHEDULE_TO_START"),
    TASK_SCHEDULE_TO_CLOSE ("TASK_SCHEDULE_TO_CLOSE"),

    TASK_POLL_TO_COMMIT("TASK_POLL_TO_COMMIT"),
    TASK_RELEASE_TO_COMMIT("TASK_RELEASE_TO_COMMIT");

    private String value;

    private TimeoutType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
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
        } else {
            throw new IllegalArgumentException("Cannot create TimeoutType for value["+value+"]");
        }
    }

}
