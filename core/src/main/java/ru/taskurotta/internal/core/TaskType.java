package ru.taskurotta.internal.core;

/**
 * User: stukushin
 * Date: 28.12.12
 * Time: 16:24
 */
public enum TaskType {
    DECIDER_START(0), DECIDER_ASYNCHRONOUS(1), WORKER(2), WORKER_SCHEDULED(3);

    int value;

    TaskType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }


    public static TaskType fromInt(int i) {
        if (i == 0) return TaskType.DECIDER_START;
        if (i == 1) return TaskType.DECIDER_ASYNCHRONOUS;
        if (i == 2) return TaskType.WORKER;
        if (i == 3) return TaskType.WORKER_SCHEDULED;
        return null;
    }

}
