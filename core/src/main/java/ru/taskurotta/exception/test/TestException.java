package ru.taskurotta.exception.test;

/**
 * User: greg
 */
public class TestException extends RuntimeException {

    public TestException(String message) {
        super(message);
    }

    public TestException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestException(Throwable cause) {
        super(cause);
    }
}
