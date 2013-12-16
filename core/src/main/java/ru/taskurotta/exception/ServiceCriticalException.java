package ru.taskurotta.exception;

/**
 * User: greg
 */

/**
 * Exception which should throw by service when something wrong happened in critical part like: SQLException, IOException
 */
public class ServiceCriticalException extends RuntimeException {

    public ServiceCriticalException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceCriticalException(String message) {
       super(message);
    }
}
