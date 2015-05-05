package ru.taskurotta.service.console.retriever;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.queue.TaskQueueItem;

import java.util.Map;

/**
 * Task queues information retriever. Provides information such as number
 * of queues, queue names, task count and such
 * Date: 17.05.13 16:05
 */
public interface QueueInfoRetriever {

    GenericPage<String> getQueueList(int pageNum, int pageSize);

    int getQueueTaskCount(String queueName);

    GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize);

    Map<String, Integer> getHoveringCount(float periodSize);

    GenericPage<QueueStatVO> getQueuesStatsPage(int pageNum, int pageSize, String filter);

    long getLastPolledTaskEnqueueTime(String queueName);

    void clearQueue(String queueName);

    void removeQueue(String queueName);

    long getQueueStorageCount(String queueName);

}
