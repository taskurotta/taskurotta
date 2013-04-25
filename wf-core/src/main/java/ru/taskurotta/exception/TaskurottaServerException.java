package ru.taskurotta.exception;

/**
 * Exeption identifies that something failed on taskurotta task server
 * User: dimadin
 * Date: 25.04.13
 * Time: 17:10
 */
public class TaskurottaServerException extends RuntimeException {

    public TaskurottaServerException() {
    }

    public TaskurottaServerException(String message) {
        super(message);
    }

    public TaskurottaServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskurottaServerException(Throwable cause) {
        super(cause);
    }
}
