package ru.taskurotta.console.retriever;

import ru.taskurotta.console.model.QueuedTaskVO;

import java.util.List;

/**
 * Task queues information retriever. Provides information such as number
 * of queues, queue names, task count and such
 * User: dimadin
 * Date: 17.05.13 16:05
 */
public interface QueueInfoRetriever {

    public List<String> getQueueList();

    public int getQueueTaskCount(String queueName);

    public List<QueuedTaskVO> getQueueContent(String queueName);

}
