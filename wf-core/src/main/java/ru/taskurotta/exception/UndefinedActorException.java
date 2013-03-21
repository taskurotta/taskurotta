package ru.taskurotta.exception;

import ru.taskurotta.core.TaskTarget;

/**
 * User: stukushin
 * Date: 14.01.13
 * Time: 13:27
 */
public class UndefinedActorException extends ActorRuntimeException {

    public UndefinedActorException(TaskTarget taskTarget) {
        super("Undefined method for " + taskTarget);
    }
}
