package ru.taskurotta.service.recovery;

import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.queue.QueueService;

/**
 * User: stukushin
 * Date: 18.09.13
 * Time: 13:09
 */
public interface QueueServiceStatistics extends QueueService, QueueInfoRetriever {
    long getLastPolledTaskEnqueueTime(String queueName);
}
