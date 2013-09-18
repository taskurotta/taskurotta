package ru.taskurotta.backend.recovery;

import ru.taskurotta.backend.queue.QueueBackend;

/**
 * User: stukushin
 * Date: 18.09.13
 * Time: 11:18
 */
public class MemoryQueueBackendStatistics extends AbstractQueueBackendStatistics {

    protected MemoryQueueBackendStatistics(QueueBackend queueBackend) {
        super(queueBackend);
    }
}
