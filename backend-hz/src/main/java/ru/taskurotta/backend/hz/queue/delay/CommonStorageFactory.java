package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicates;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 04.12.13
 * Time: 17:33
 */
public class CommonStorageFactory implements StorageFactory {

    private final IMap<String, StorageItem> iMap;

    public CommonStorageFactory(final HazelcastInstance hazelcastInstance, String commonStorageName) {
        this.iMap = hazelcastInstance.getMap(commonStorageName);
        this.iMap.addIndex("keepTime", true);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Set<String> keys = iMap.keySet(new Predicates.BetweenPredicate("keepTime", 0l, System.currentTimeMillis()));

                    if (keys == null || keys.isEmpty()) {
                        continue;
                    }

                    for (String key : keys) {
                        StorageItem storageItem = iMap.remove(key);
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
            public boolean add(Object o, int delayTime, TimeUnit unit) {
                StorageItem storageItem = new StorageItem(o, queueName, System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delayTime, unit));

                String key = queueName + "." + storageItem.hashCode() + "." + System.currentTimeMillis();

                return iMap.tryPut(key, storageItem, 1, TimeUnit.SECONDS);
            }

            @Override
            public Collection getReadyItems() {
                return null;
            }
        };
    }

}
