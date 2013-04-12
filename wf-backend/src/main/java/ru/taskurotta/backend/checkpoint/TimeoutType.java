package ru.taskurotta.backend.checkpoint;

public enum TimeoutType {

    WORKFLOW_START_TO_CLOSE ("WORKFLOW_START_TO_CLOSE"),
    TASK_START_TO_CLOSE ("TASK_START_TO_CLOSE"),
    TASK_SCHEDULE_TO_START("TASK_SCHEDULE_TO_START"),
    TASK_SCHEDULE_TO_CLOSE ("TASK_SCHEDULE_TO_CLOSE");

    private String value;

    private TimeoutType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static TimeoutType forValue(String value) {
        if("WORKFLOW_START_TO_CLOSE".equalsIgnoreCase(value)) {
            return TimeoutType.WORKFLOW_START_TO_CLOSE;
        } else if("TASK_START_TO_CLOSE".equalsIgnoreCase(value)) {
            return TimeoutType.TASK_START_TO_CLOSE;
        } else if("TASK_SCHEDULE_TO_START".equalsIgnoreCase(value)) {
            return TimeoutType.TASK_SCHEDULE_TO_START;
        } else if("TASK_SCHEDULE_TO_CLOSE".equalsIgnoreCase(value)) {
            return TimeoutType.TASK_SCHEDULE_TO_CLOSE;
        } else {
            throw new IllegalArgumentException("Cannot create TimeoutType for value["+value+"]");
        }
    }

}
