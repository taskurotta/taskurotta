package ru.taskurotta.server;

import java.util.UUID;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskStateObject;
import ru.taskurotta.server.config.expiration.ExpirationPolicy;
import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.util.ActorDefinition;

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

    /**
     * Method should find all tasks with given actorId and expired "process" state
     * and send them to queue
     *
     * @param actorQueueId
     * @param expPolicy
     * @return
     */
    public int reScheduleTasks(String actorQueueId, ExpirationPolicy expPolicy);

}
