package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicates;

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
public class CommonStorageFactory implements StorageFactory {

    private final IMap<UUID, StorageItem> iMap;

    public CommonStorageFactory(final HazelcastInstance hazelcastInstance, String commonStorageName, long scheduleDelayMillis) {
        this.iMap = hazelcastInstance.getMap(commonStorageName);
        this.iMap.addIndex("enqueueTime", true);

        ScheduledExecutorService singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("CommonStorageFactory-" + counter++);
                return thread;
            }
        });

        singleThreadScheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (iMap.isEmpty()) {
                    return;
                }

                Set<UUID> keys = iMap.localKeySet(new Predicates.BetweenPredicate("enqueueTime", 0l,
                        System.currentTimeMillis()));

                if (keys == null || keys.isEmpty()) {
                    return;
                }

                for (UUID key : keys) {
                    StorageItem storageItem = iMap.remove(key);
                    String queueName = storageItem.getQueueName();
                    hazelcastInstance.getQueue(queueName).add(storageItem.getObject());
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
        };
    }

    private boolean save(Object o, long delayTime, TimeUnit unit, String queueName) {
        long enqueueTime = System.currentTimeMillis() + unit.toMillis(delayTime);
        StorageItem storageItem = new StorageItem(o, enqueueTime, queueName);

        while (iMap.putIfAbsent(UUID.randomUUID(), storageItem) != null) {
            // Better safe than sorry! :)
        }

        return true;
    }

    private boolean delete(Object o) {
        UUID key = null;

        for (Map.Entry<UUID, StorageItem> entry : iMap.entrySet()) {
            if (entry.getValue().equals(o)) {
                key = entry.getKey();
                break;
            }
        }

        return key != null && iMap.remove(key, o);
    }
}
