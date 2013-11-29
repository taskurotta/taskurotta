package ru.taskurotta.exception;

/**
 * User: stukushin
 * Date: 24.01.13
 * Time: 17:42
 */
public class TargetException extends ActorRuntimeException {
    public TargetException() {
    }

    public TargetException(String message) {
        super("Can't call method " + message);
    }

    public TargetException(String message, Throwable cause) {
        super("Can't call method " + message, cause);
    }

    public TargetException(Throwable cause) {
        super(cause);
    }
}
