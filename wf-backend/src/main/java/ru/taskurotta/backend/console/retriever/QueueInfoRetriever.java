package ru.taskurotta.backend.console.retriever;

import java.util.Map;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueuedTaskVO;

/**
 * Task queues information retriever. Provides information such as number
 * of queues, queue names, task count and such
 * User: dimadin
 * Date: 17.05.13 16:05
 */
public interface QueueInfoRetriever {

    public GenericPage<String> getQueueList(int pageNum, int pageSize);

    public int getQueueTaskCount(String queueName);

    public GenericPage<QueuedTaskVO> getQueueContent(String queueName, int pageNum, int pageSize);

    public Map<String, Integer> getHoveringCount(float periodSize);

}
