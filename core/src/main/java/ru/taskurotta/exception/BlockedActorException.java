package ru.taskurotta.exception;

import ru.taskurotta.util.ActorDefinition;

/**
 * User: stukushin
 * Date: 19.07.13
 * Time: 14:24
 */
public class BlockedActorException extends ActorRuntimeException {

    public BlockedActorException(String actorId) {
        super("Blocked actor " + actorId);
    }
}
