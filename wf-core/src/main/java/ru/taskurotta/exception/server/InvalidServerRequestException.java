package ru.taskurotta.exception.server;

/**
 * Marker exception specifying that client response was invalid
 * User: dimadin
 * Date: 29.04.13 15:18
 */
public class InvalidServerRequestException extends ServerException {
    public InvalidServerRequestException() {
    }

    public InvalidServerRequestException(String message) {
        super(message);
    }

    public InvalidServerRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidServerRequestException(Throwable cause) {
        super(cause);
    }
}
