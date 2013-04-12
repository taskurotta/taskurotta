package ru.taskurotta.exception;

/**
 * User: romario
 * Date: 1/11/13
 * Time: 2:24 PM
 */
public class IncorrectExecuteMethodDefinition extends ActorRuntimeException {

    public IncorrectExecuteMethodDefinition(String message, Object decider) {
        super(message + " : " + decider.getClass().getName());
    }

    public IncorrectExecuteMethodDefinition(String message, Class deciderClass) {
        super(message + " : " + deciderClass.getName());
    }
}
