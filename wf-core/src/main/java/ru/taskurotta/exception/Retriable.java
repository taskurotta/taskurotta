package ru.taskurotta.exception;

/**
 * Marker exception that indicates that operation retry required
 */
public class Retriable extends RuntimeException {

    public Retriable() {
    }

    public Retriable(String message) {
        super(message);
    }

    public Retriable(String message, Throwable cause) {
        super(message, cause);
    }

    public Retriable(Throwable cause) {
        super(cause);
    }
}
