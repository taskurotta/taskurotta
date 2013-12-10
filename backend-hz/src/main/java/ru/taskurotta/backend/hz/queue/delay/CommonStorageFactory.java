package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicates;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 04.12.13
 * Time: 17:33
 */
public class CommonStorageFactory implements StorageFactory {

    private final IMap<UUID, CommonStorageItem> iMap;

    public CommonStorageFactory(final HazelcastInstance hazelcastInstance, String commonStorageName) {
        this.iMap = hazelcastInstance.getMap(commonStorageName);
        this.iMap.addIndex("enqueueTime", true);

        ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("CommonStorageFactory-" + counter++);
                return thread;
            }
        });

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    if (iMap.isEmpty()) {
                        continue;
                    }

                    Set<UUID> keys = iMap.localKeySet(new Predicates.BetweenPredicate("enqueueTime", 0l,
                            System.currentTimeMillis()));

                    if (keys == null || keys.isEmpty()) {
                        continue;
                    }

                    for (UUID key : keys) {
                        CommonStorageItem storageItem = iMap.remove(key);
                        String queueName = storageItem.getQueueName();
                        hazelcastInstance.getQueue(queueName).add(storageItem.getObject());
                    }
                }
            }
        });
    }

    @Override
    public Storage createStorage(final String queueName) {
        return new Storage() {
            @Override
            public boolean add(Object o, long delayTime, TimeUnit unit) {

                long enqueueTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delayTime, unit);
                CommonStorageItem storageItem = new CommonStorageItem(o, enqueueTime, queueName);

                while (iMap.putIfAbsent(UUID.randomUUID(), storageItem) != null) {
                    // Better safe than sorry! :)
                }

                return true;
            }

            @Override
            public boolean remove(Object o) {
                UUID key = null;

                for (Map.Entry<UUID, CommonStorageItem> entry : iMap.entrySet()) {
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
