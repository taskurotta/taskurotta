package ru.taskurotta.exception;

/**
 * Error at (de)serializing container entities
 * User: dimadin
 * Date: 18.07.13 16:27
 */
public class SerializationException extends RuntimeException {

    public SerializationException() {
    }

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }

}
