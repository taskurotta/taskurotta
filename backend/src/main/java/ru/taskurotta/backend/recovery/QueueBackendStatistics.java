package ru.taskurotta.backend.recovery;

import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.queue.QueueBackend;

/**
 * User: stukushin
 * Date: 18.09.13
 * Time: 13:09
 */
public interface QueueBackendStatistics extends QueueBackend, QueueInfoRetriever {
    long getLastPolledTaskEnqueueTime(String queueName);
}
