package ru.taskurotta.exception;

/**
 * User: greg
 */

/**
 * Exception which should throw by backend when something wrong happened in critical part like: SQLException, IOException
 */
public class BackendCriticalException extends RuntimeException {

    public BackendCriticalException(String message, Throwable cause) {
        super(message, cause);
    }

    public BackendCriticalException(String message) {
       super(message);
    }
}
