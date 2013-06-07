package ru.taskurotta.backend.queue;

import ru.taskurotta.backend.checkpoint.CheckpointServiceProvider;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:12 PM
 */
public interface QueueBackend extends CheckpointServiceProvider {

    /**
     * Create TASK_POLL_TIMEOUT checkpoint.
     *
     * @param actorId
     * @return
     */
    public UUID poll(String actorId, String taskList);


    /**
     * Create TASK_TIMEOUT checkpoint
     * Delete TASK_POLL_TIMEOUT checkpoint
     *
     * @param taskId
     */
    public void pollCommit(String actorId, UUID taskId);


    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList);

}
