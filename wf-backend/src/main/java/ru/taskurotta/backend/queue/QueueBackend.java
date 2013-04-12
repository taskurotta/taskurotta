package ru.taskurotta.backend.queue;

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
     * @param actorId
     * @return
     */
    public UUID poll(String actorId);


    /**
     * Create TASK_TIMEOUT checkpoint
     * Delete TASK_POLL_TIMEOUT checkpoint
     *
     * @param taskId
     */
    public void pollCommit(String actorId, UUID taskId);


    public void enqueueItem(String actorId, UUID taskId, long startTime);

}
