package ru.taskurotta.exception.server;

/**
 * Exeption identifies that something failed on taskurotta task server request
 * User: dimadin
 * Date: 25.04.13
 * Time: 17:10
 */
public class ServerException extends RuntimeException {

    public ServerException() {
    }

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerException(Throwable cause) {
        super(cause);
    }
}
