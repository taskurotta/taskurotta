package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;
import ru.taskurotta.hazelcast.queue.CachedQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:19 AM
 */
public class DefaultQueueFactory implements QueueFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultQueueFactory.class);

    private HazelcastInstance hazelcastInstance;
    private StorageFactory storageFactory;
    private HzQueueConfigSupport hzQueueConfigSupport;

    private transient final ReentrantLock lock = new ReentrantLock();
    private ConcurrentHashMap<String, CachedDelayQueue> queueMap = new ConcurrentHashMap<>();


    public DefaultQueueFactory(HazelcastInstance hazelcastInstance, StorageFactory storageFactory) {
        this.hazelcastInstance = hazelcastInstance;
        this.storageFactory = storageFactory;
    }

    public DefaultQueueFactory(HazelcastInstance hazelcastInstance, StorageFactory storageFactory, HzQueueConfigSupport hzQueueConfigSupport) {
        this.hazelcastInstance = hazelcastInstance;
        this.storageFactory = storageFactory;
        this.hzQueueConfigSupport = hzQueueConfigSupport;
    }

    @Override
    public CachedDelayQueue create(String queueName) {

        CachedDelayQueue cachedDelayQueue = queueMap.get(queueName);

        if (cachedDelayQueue == null) {

            final ReentrantLock lock = this.lock;
            lock.lock();

            try {
                cachedDelayQueue = queueMap.get(queueName);
                if (cachedDelayQueue != null) {
                    return cachedDelayQueue;
                }

                if (hzQueueConfigSupport != null) {
                    hzQueueConfigSupport.createQueueConfig(queueName);
                } else {
                    logger.warn("HzQueueConfigSupport is not configured");
                }

                CachedQueue cQueue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), queueName);
                Storage storage = storageFactory.createStorage(queueName);

                cachedDelayQueue = new CachedDelayQueueImpl(cQueue, storage);

                // Do not remove commented code! This is example of deadlock for future analyze
//                try {
//                    delayIQueue.put(UUID.randomUUID());
//                } catch (InterruptedException e) {
//                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                }

                queueMap.put(queueName, cachedDelayQueue);
            } finally {
                lock.unlock();
            }
        }

        return cachedDelayQueue;
    }
}
