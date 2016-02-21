package ru.taskurotta.server.test;

import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

/**
 */
public class MockDecision extends TaskDecisionImpl {

    String actorName;
    String actorVersion;
    String actorMethod;
    String actorTaskList;
    private ActorDefinition actorDefinition;

    public MockDecision(String actorId) {

        // parse task target from actorId

        String[] parts = actorId.split("#");
        if (parts.length < 3 || parts.length > 4) {
            throw new IllegalArgumentException("actorId = " + actorId);
        }

        this.actorName = parts[0];
        this.actorVersion = parts[1];
        this.actorMethod = parts[2];

        if (parts.length == 4) {
            this.actorTaskList = parts[3];
        }

        this.actorDefinition = ActorDefinition.valueOf(actorName, actorVersion, actorTaskList);
        this.executionTime = 1;

    }

    public ActorDefinition getActorDefinition() {
        return actorDefinition;
    }

    public void correspondsTo(TaskContainer taskContainer) {
        this.processId = taskContainer.getProcessId();
        this.taskId = taskContainer.getTaskId();
    }

    public String getActorId() {
        return ActorUtils.getActorId(actorDefinition);
    }

    public MockDecision setException(Throwable throwable) {
        this.exception = throwable;
        this.error = true;
        return this;
    }
}
