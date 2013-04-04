package ru.taskurotta.backend.queue;

import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:12 PM
 */
public interface QueueBackend {

    /**
     * Create TASK_POLL_TIMEOUT checkpoint.
     *
     * @param actorDefinition
     * @return
     */
    public UUID poll(ActorDefinition actorDefinition);


    /**
     * Create TASK_TIMEOUT checkpoint
     * Delete TASK_POLL_TIMEOUT checkpoint
     *
     * @param taskId
     */
    public void pollCommit(UUID taskId);


    public void enqueueItem(ActorDefinition actorDefinition, UUID taskId, long startTime);

}
