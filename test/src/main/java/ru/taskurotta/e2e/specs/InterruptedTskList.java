package ru.taskurotta.e2e.specs;

import ru.taskurotta.e2e.SpecSuite;
import ru.taskurotta.server.test.ActorEngine;
import ru.taskurotta.server.test.MockDecision;
import ru.taskurotta.server.test.MockTask;

/**
 */
public class InterruptedTskList implements SpecSuite {

    public static final String START = "e2e.Decider#1.0#start";

    ActorEngine actorEngine;

    public InterruptedTskList(ActorEngine actorEngine) {
        this.actorEngine = actorEngine;
    }


    @Override
    public void init() {

        actorEngine.startProcess(new MockTask(START));
        actorEngine.executeActor(new MockDecision(START));
    }

    @Override
    public void clean() {

    }
}
