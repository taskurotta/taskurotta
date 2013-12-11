package ru.taskurotta.hazelcast.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(CommonStorageFactory.class);

    private final IMap<UUID, StorageItem> iMap;

    public CommonStorageFactory(final HazelcastInstance hazelcastInstance, String commonStorageName, String schedule) {
        this.iMap = hazelcastInstance.getMap(commonStorageName);
        this.iMap.addIndex("enqueueTime", true);

        long delay = 1000l;
        TimeUnit delayTimeUnit = TimeUnit.MILLISECONDS;
        String[] params = schedule.split("_");
        if (params.length == 2) {
            delay = Long.valueOf(params[0]);
            delayTimeUnit = TimeUnit.valueOf(params[1].toUpperCase());
        }
        logger.info("Set schedule delay = [{}] delayTimeUnit = [{}] for search ready processes for GC", delay, delayTimeUnit);

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
        }, 0l, delay, delayTimeUnit);
    }

    @Override
    public Storage createStorage(final String queueName) {
        return new Storage() {
            @Override
            public boolean add(Object o, long delayTime, TimeUnit unit) {

                long enqueueTime = System.currentTimeMillis() + unit.toMillis(delayTime);
                StorageItem storageItem = new StorageItem(o, enqueueTime, queueName);

                while (iMap.putIfAbsent(UUID.randomUUID(), storageItem) != null) {
                    // Better safe than sorry! :)
                }

                return true;
            }

            @Override
            public boolean remove(Object o) {
                UUID key = null;

                for (Map.Entry<UUID, StorageItem> entry : iMap.entrySet()) {
                    if (entry.getValue().equals(o)) {
                        key = entry.getKey();
                        break;
                    }
                }

                return key != null && iMap.remove(key, o);
            }

            @Override
            public boolean contains(Object o) {
                return iMap.containsValue(o);
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
}
