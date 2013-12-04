package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;

import java.util.Collection;

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

        if (false) {
            // start thread with this loop
            // create property to tune frequency of requests (sleep time between requests)
            // create property to tune fail sleeptime (how long sleep on fail)
            // create boolean property: notSleepIfStorageFull  - ugly name
            // properties has default values. Create second constructor with this properties to override default
            // values.
            while (true) {

                try {
                    Collection readyItems = storage.getReadyItems();
                    for (Object item: readyItems) {
                        queue.add(item);
                    }

                } catch (Throwable ex) {
                    // catch everything, report and sleep with peace
                }
            }
        }

        return new DelayIQueue(queue, storage);
    }
}
