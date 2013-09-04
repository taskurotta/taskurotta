package ru.taskurotta.exception.server;

/**
 * Marker exception specifying that some network communication errors occurred: connection reset, timeout and so on
 * User: dimadin
 * Date: 29.04.13 15:04
 */
public class ServerConnectionException extends ServerException {
    public ServerConnectionException() {
    }

    public ServerConnectionException(String message) {
        super(message);
    }

    public ServerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerConnectionException(Throwable cause) {
        super(cause);
    }
}
