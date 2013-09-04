package ru.taskurotta.exception;

/**
 * User: romario
 * Date: 1/11/13
 * Time: 2:24 PM
 */
public class IncorrectAsynchronousMethodDefinition extends ActorRuntimeException {

    public IncorrectAsynchronousMethodDefinition(String message, Class deciderClass) {
        super(message + " : " + deciderClass.getName());
    }

}
