package ru.taskurotta.server;

import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.server.model.TaskStateObject;
import ru.taskurotta.server.transport.ArgContainer;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 12:57 PM
 */
public interface TaskDao {

    public TaskObject pull(ActorDefinition actorDefinition);

    public void add(TaskObject task);

    public ArgContainer getTaskValue(UUID taskId);

    public void decrementCountdown(UUID taskId, int decrementValue);

    public void logTaskResult(DecisionContainer taskResult);

    public void unlogTaskResult(UUID taskId);

    public void saveTaskValue(UUID taskId, ArgContainer value, TaskStateObject taskState);

    public TaskObject findById(UUID taskId);

    /**
     * @param taskId
     * @param externalWaitForTaskId
     * @return true if registration successfully processed
     */
    public boolean registerExternalWaitFor(UUID taskId, UUID externalWaitForTaskId);

}
