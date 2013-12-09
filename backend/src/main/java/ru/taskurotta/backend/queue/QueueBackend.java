package ru.taskurotta.backend.queue;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:12 PM
 */
public interface QueueBackend {

    public TaskQueueItem poll(String actorId, String taskList);

    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList);

    /**
     * TODO: remove this method. Should be implemented only in MemoryQueueBackend for testing purpose.
     * @param actorId
     * @param taskList
     * @param taskId
     * @param processId
     * @return
     */
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId);

    public String createQueueName(String actorId, String taskList);

}
