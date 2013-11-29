package ru.taskurotta.exception;

/**
 * User: stukushin
 * Date: 23.01.13
 * Time: 14:22
 */
public class TaskTargetRequiredException extends ActorRuntimeException {
    public TaskTargetRequiredException(String beanName) {
        super("Actor bean " + beanName + " has no required target methods");
    }
}
