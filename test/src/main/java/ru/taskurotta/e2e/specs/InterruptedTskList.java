package ru.taskurotta.e2e.specs;

import ru.taskurotta.e2e.SpecSuite;
import ru.taskurotta.server.test.ActorEngine;
import ru.taskurotta.server.test.MockDecision;
import ru.taskurotta.server.test.MockStartTask;

/**
 */
public class InterruptedTskList implements SpecSuite {

    public static final String DECIDER = "e2e.Decider#1.0#start#list";
    public static final String DECIDER2 = "e2e.ProcessManager#1.0#start";

    ActorEngine actorEngine;

    public InterruptedTskList(ActorEngine actorEngine) {
        this.actorEngine = actorEngine;
    }


    @Override
    public void init() {

        actorEngine.startProcess(new MockStartTask(DECIDER), 5);
        actorEngine.executeActor(
                new MockDecision(DECIDER)
                        .setException(new IllegalAccessError("Permission deny")));
        actorEngine.executeActor(
                new MockDecision(DECIDER)
                        .setException(new IllegalStateException("Object not found")));
        actorEngine.executeActor(
                new MockDecision(DECIDER));
        actorEngine.pollTask(
                new MockDecision(DECIDER));


        actorEngine.startProcess(new MockStartTask(DECIDER2), 2);
        actorEngine.executeActor(
                new MockDecision(DECIDER2)
                        .setException(new IllegalAccessError("Permission deny")));
        actorEngine.executeActor(
                new MockDecision(DECIDER2)
                        .setException(new IllegalStateException("Object not found")));
    }

    @Override
    public void clean() {

    }
}
