package ru.taskurotta.service.queue;

import java.util.Collection;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:12 PM
 */
public interface QueueService {

    TaskQueueItem poll(String actorId, String taskList);

    boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList);

    /**
     * TODO: remove this method. Should be implemented only in MemoryQueueService for testing purpose.
     * @param actorId
     * @param taskList
     * @param taskId
     * @param processId
     * @return
     */
    boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId);

    String createQueueName(String actorId, String taskList);

    long getLastPolledTaskEnqueueTime(String queueName);

    Collection<String> getQueueNames();

    Collection<String> getQueueNames(String prefix, String filter, boolean prefixStrip);

}
