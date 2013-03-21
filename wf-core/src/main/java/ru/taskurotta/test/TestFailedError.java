package ru.taskurotta.test;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 11:49 PM
 */
public class TestFailedError extends RuntimeException {

    public TestFailedError(String msg) {
        super(msg);
    }

    public TestFailedError(String msg, Throwable cause) {
        super(msg, cause);
    }
}