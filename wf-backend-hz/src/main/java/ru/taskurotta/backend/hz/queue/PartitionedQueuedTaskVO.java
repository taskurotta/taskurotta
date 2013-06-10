package ru.taskurotta.backend.hz.queue;

import com.hazelcast.core.PartitionAware;
import ru.taskurotta.backend.console.model.QueuedTaskVO;

/**
 * POJO representing item of a distributed queue with partitioning
 * User: dimadin
 * Date: 07.06.13 11:23
 */
public class PartitionedQueuedTaskVO extends QueuedTaskVO implements PartitionAware {

    private Object partitionKey;

    public void setPartitionKey(Object partitionKey) {
        this.partitionKey = partitionKey;
    }

    @Override
    public Object getPartitionKey() {
        return partitionKey;
    }

    @Override
    public String toString() {
        return "PartitionedQueuedTaskVO{" +
                "partitionKey=" + partitionKey +
                "} " + super.toString();
    }
}
