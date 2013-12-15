package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:19 AM
 */
public class BaseQueueFactory implements QueueFactory {

    private HazelcastInstance hazelcastInstance;
    private StorageFactory storageFactory;
    private HzQueueConfigSupport hzQueueConfigSupport;

    private transient final ReentrantLock lock = new ReentrantLock();
    private ConcurrentHashMap<String, DelayIQueue> queueMap = new ConcurrentHashMap<>();

    public BaseQueueFactory(HazelcastInstance hazelcastInstance, StorageFactory storageFactory) {
        this.hazelcastInstance = hazelcastInstance;
        this.storageFactory = storageFactory;
    }

    public BaseQueueFactory(HazelcastInstance hazelcastInstance, StorageFactory storageFactory, HzQueueConfigSupport hzQueueConfigSupport) {
        this.hazelcastInstance = hazelcastInstance;
        this.storageFactory = storageFactory;
        this.hzQueueConfigSupport = hzQueueConfigSupport;
    }

    @Override
    public DelayIQueue create(String queueName) {

        DelayIQueue delayIQueue = queueMap.get(queueName);

        if (delayIQueue == null) {

            final ReentrantLock lock = this.lock;
            lock.lock();

            try {
                delayIQueue = queueMap.get(queueName);
                if (delayIQueue != null) {
                    return delayIQueue;
                }

                if (hzQueueConfigSupport != null) {
                    hzQueueConfigSupport.createQueueConfig(queueName);
                }

                IQueue iQueue = hazelcastInstance.getQueue(queueName);
                Storage storage = storageFactory.createStorage(queueName);

                delayIQueue = new DelayIQueue(iQueue, storage);

                // Do not remove commented code! This is example of deadlock for future analyze
//                try {
//                    delayIQueue.put(UUID.randomUUID());
//                } catch (InterruptedException e) {
//                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                }

                queueMap.put(queueName, delayIQueue);
            } finally {
                lock.unlock();
            }
        }

        return delayIQueue;
    }
}
