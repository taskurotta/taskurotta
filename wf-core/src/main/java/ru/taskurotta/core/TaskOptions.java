package ru.taskurotta.core;

import ru.taskurotta.transport.model.ArgType;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 16:24
 */
public interface TaskOptions {
    ArgType[] getArgTypes();
    ActorSchedulingOptions getActorSchedulingOptions();
    Promise<?>[] getPromisesWaitFor();
}
