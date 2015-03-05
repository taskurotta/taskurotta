package ru.taskurotta.test.stress;

/**
 */
public class BrokenProcessException extends RuntimeException {

    public BrokenProcessException() {
    }

    public BrokenProcessException(String message) {
        super(message);
    }

    public BrokenProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BrokenProcessException(Throwable cause) {
        super(cause);
    }

    public BrokenProcessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
