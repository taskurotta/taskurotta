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
     * Task should be polled and marked as "fly" until pollCommit or until specified timeout.
     * If timeout has occur than task should be returned to queue
     *
     * @param actorDefinition
     * @return
     */
    public UUID poll(ActorDefinition actorDefinition);


    /**
     * Remove "fly" marker from the task
     *
     * @param taskId
     */
    public void pollCommit(UUID taskId);


    public void enqueueItem(ActorDefinition actorDefinition, UUID taskId, long startTime);

}
