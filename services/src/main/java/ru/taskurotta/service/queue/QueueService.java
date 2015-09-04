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

    String createQueueName(String actorId, String taskList);

    long getLastPolledTaskEnqueueTime(String queueName);

    Collection<String> getQueueNames();

}
