package ru.taskurotta.exception;

/**
 * User: stukushin
 * Date: 24.01.13
 * Time: 18:07
 */
public class IllegalClientImplementationException extends ActorRuntimeException {
    public IllegalClientImplementationException(String message) {
        super(message);
    }
}
