package ru.taskurotta.exception;

/**
 * Indicates that implementing class is aware if its execution retry policy
 */
public interface Retriable {

    boolean isShouldBeRestarted();

    long getRestartTime();

}
