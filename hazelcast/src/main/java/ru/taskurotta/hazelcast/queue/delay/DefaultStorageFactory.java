package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.delay.impl.StorageItemContainer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 04.12.13
 * Time: 17:33
 */
public class DefaultStorageFactory implements StorageFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultStorageFactory.class);

    private final IMap<UUID, StorageItemContainer> iMap;

    public static final String ENQUEUE_TIME_NAME = "enqueueTime";

    public DefaultStorageFactory(final HazelcastInstance hazelcastInstance, String commonStorageName, long scheduleDelayMillis) {
        this.iMap = hazelcastInstance.getMap(commonStorageName);
        this.iMap.addIndex(ENQUEUE_TIME_NAME, true);

        ScheduledExecutorService singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("CommonStorageFactory-" + counter++);
                thread.setDaemon(true);
                return thread;
            }
        });

        singleThreadScheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (iMap.isEmpty()) {
                        return;
                    }

                    Set<UUID> keys = iMap.localKeySet(Predicates.between(ENQUEUE_TIME_NAME, 0l,
                            System.currentTimeMillis()));

                    if (keys == null || keys.isEmpty()) {
                        return;
                    }

                    logger.debug("find new {} items to queue", keys.size());

                    for (UUID key : keys) {
                        StorageItemContainer storageItemContainer = iMap.get(key);
                        String queueName = storageItemContainer.getQueueName();

                        CachedQueue cQueue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(),
                                queueName);

                        if (cQueue.offer(storageItemContainer.getObject())) {
                            iMap.delete(key);
                        }
                    }
                } catch (HazelcastInstanceNotActiveException ignored) {
                } catch (Throwable e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        }, 0l, scheduleDelayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Storage createStorage(final String queueName) {
        return new Storage() {
            @Override
            public boolean add(Object o, long delayTime, TimeUnit unit) {
                return save(o, delayTime, unit, queueName);
            }

            @Override
            public boolean remove(Object o) {
                return delete(o);
            }

            @Override
            public void clear() {
                // don't clear, because it's common storage for all queues
            }

            @Override
            public void destroy() {
                // don't destroy, because it's common storage for all queues
            }

            @Override
            public long size() {
                int result = 0;

                for (StorageItemContainer st : iMap.values()) {
                    if (st != null && st.getQueueName().equals(queueName)) {
                        result++;
                    }
                }

                return result;
            }
        };
    }

    private boolean save(Object o, long delayTime, TimeUnit unit, String queueName) {
        long enqueueTime = System.currentTimeMillis() + unit.toMillis(delayTime);
        StorageItemContainer storageItemContainer = new StorageItemContainer(UUID.randomUUID(), o, enqueueTime,
                queueName);

        while (iMap.putIfAbsent(UUID.randomUUID(), storageItemContainer) != null) {
            // Better safe than sorry! :)
        }

        return true;
    }

    private boolean delete(Object o) {
        UUID key = null;

        for (Map.Entry<UUID, StorageItemContainer> entry : iMap.entrySet()) {
            if (entry.getValue().equals(o)) {
                key = entry.getKey();
                break;
            }
        }

        return key != null && iMap.remove(key, o);
    }
}
