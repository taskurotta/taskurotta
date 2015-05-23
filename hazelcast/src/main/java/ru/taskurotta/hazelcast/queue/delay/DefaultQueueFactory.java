package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.delay.impl.CachedDelayQueueImpl;

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

    private transient final ReentrantLock lock = new ReentrantLock();
    private ConcurrentHashMap<String, CachedDelayQueue> queueMap = new ConcurrentHashMap<>();


    public DefaultQueueFactory(HazelcastInstance hazelcastInstance, StorageFactory storageFactory) {
        this.hazelcastInstance = hazelcastInstance;
        this.storageFactory = storageFactory;
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
