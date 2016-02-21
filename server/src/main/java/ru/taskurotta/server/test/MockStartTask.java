package ru.taskurotta.server.test;

import ru.taskurotta.internal.core.TaskType;

/**
 */
public class MockStartTask extends MockTask {

    public MockStartTask(String actorId) {
        super(actorId);
        setType(TaskType.DECIDER_START);
    }
}
