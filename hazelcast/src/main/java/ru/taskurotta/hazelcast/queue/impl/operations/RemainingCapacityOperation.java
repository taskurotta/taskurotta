package ru.taskurotta.hazelcast.queue.impl.operations;


import ru.taskurotta.hazelcast.queue.impl.QueueContainer;
import ru.taskurotta.hazelcast.queue.impl.QueueDataSerializerHook;
import ru.taskurotta.hazelcast.queue.impl.stats.LocalCachedQueueStatsImpl;

/**
 * Returns the remaining capacity of the queue based on config max-size
 */
public class RemainingCapacityOperation extends QueueOperation {

    public RemainingCapacityOperation() {
    }

    public RemainingCapacityOperation(final String name) {
        super(name);
    }

    @Override
    public void run() {
        final QueueContainer container = getOrCreateContainer();
        response = container.getConfig().getCacheSize() - container.size();
    }

    @Override
    public void afterRun() throws Exception {
        LocalCachedQueueStatsImpl stats = getQueueService().getLocalQueueStatsImpl(name);
        stats.incrementOtherOperations();
    }


    @Override
    public int getId() {
        return QueueDataSerializerHook.REMAINING_CAPACITY;
    }

}
