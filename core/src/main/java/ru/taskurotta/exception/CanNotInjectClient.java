package ru.taskurotta.exception;

/**
 * User: romario
 * Date: 1/14/13
 * Time: 4:51 PM
 */
public class CanNotInjectClient extends ActorRuntimeException {

    public CanNotInjectClient(String msg) {
        super(msg);
    }

    public CanNotInjectClient(String msg, Throwable cause) {
        super(msg, cause);
    }
}
