package ru.taskurotta.server;

/**
 * Supported metrics
 * User: dimadin
 * Date: 30.09.13 19:09
 */
public enum MetricName {

    START_PROCESS("startProcess"),
    POLL("poll"),
    SUCCESSFUL_POLL("successfulPoll"),
    RELEASE("release"),
    EXECUTION_TIME("executionTime"),
    ERROR_DECISION("errorDecision"),
    ENQUEUE("enqueue"),
    QUEUE_SIZE("queueSize");


    private String value;

    MetricName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
