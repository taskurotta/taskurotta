package ru.taskurotta.e2e.specs;

import ru.taskurotta.e2e.SpecSuite;
import ru.taskurotta.server.test.ActorEngine;
import ru.taskurotta.server.test.MockDecision;
import ru.taskurotta.server.test.MockStartTask;

/**
 */
public class InterruptedTskList implements SpecSuite {

    public static final String DECIDER = "e2e.Decider#1.0#start";

    ActorEngine actorEngine;

    public InterruptedTskList(ActorEngine actorEngine) {
        this.actorEngine = actorEngine;
    }


    @Override
    public void init() {

        actorEngine.startProcess(new MockStartTask(DECIDER), 3);
        actorEngine.executeActor(
                new MockDecision(DECIDER)
                        .setException(new IllegalAccessError("Permission deny")));
        actorEngine.executeActor(
                new MockDecision(DECIDER)
                        .setException(new IllegalStateException("Object not found")));
        actorEngine.executeActor(
                new MockDecision(DECIDER));
    }

    @Override
    public void clean() {

    }
}
