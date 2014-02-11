package ru.taskurotta.service.console.retriever;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.queue.TaskQueueItem;

import java.util.List;
import java.util.Map;

/**
 * Task queues information retriever. Provides information such as number
 * of queues, queue names, task count and such
 * Date: 17.05.13 16:05
 */
public interface QueueInfoRetriever {

    public GenericPage<String> getQueueList(int pageNum, int pageSize);

    public int getQueueTaskCount(String queueName);

    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize);

    public Map<String, Integer> getHoveringCount(float periodSize);

    public GenericPage<QueueStatVO> getQueuesStatsPage(int pageNum, int pageSize, String filter);

    public List<String> getQueueNames();

    public long getLastPolledTaskEnqueueTime(String queueName);

    public void clearQueue(String queueName);

    public void removeQueue(String queueName);

    public long getQueueStorageCount(String queueName);

}
