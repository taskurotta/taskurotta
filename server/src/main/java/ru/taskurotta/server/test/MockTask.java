package ru.taskurotta.server.test;

import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.core.TaskType;

import java.util.UUID;

/**
 */
public class MockTask extends TaskImpl {

    public MockTask(String actorId) {

        // default values

        TaskType taskType = TaskType.DECIDER;

        // parse task target from actorId

        String[] parts = actorId.split("#");
        if (parts.length < 3) {
            throw new IllegalArgumentException("actorId = " + actorId);
        }

        String actorName = parts[0];
        String actorVersion = parts[1];
        String actorMethod = parts[2];

        // todo: add taskList as a part[3]

        target = new TaskTargetImpl(taskType, actorName, actorVersion, actorMethod);
    }

    public MockTask setType(TaskType type) {
        target = new TaskTargetImpl(type, target.getName(), target.getVersion(), target.getMethod());
        return this;
    }

    public void generateNewIds() {
        processId = UUID.randomUUID();
        id = UUID.randomUUID();
    }

}
