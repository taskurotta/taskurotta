package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:19 AM
 */
public class BaseQueueFactory implements QueueFactory {

    private HazelcastInstance hazelcastInstance;
    private StorageFactory storageFactory;

    public BaseQueueFactory(HazelcastInstance hazelcastInstance, StorageFactory storageFactory) {
        this.hazelcastInstance = hazelcastInstance;
        this.storageFactory = storageFactory;
    }

    @Override
    public DelayIQueue create(String queueName) {

        IQueue queue = hazelcastInstance.getQueue(queueName);
        Storage storage = storageFactory.createStorage(queueName);

        return new DelayIQueue(queue, storage);
    }
}
