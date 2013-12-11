package ru.taskurotta.hazelcast.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicates;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:40 AM
 */
public class BaseStorageFactory implements StorageFactory {

    private HazelcastInstance hazelcastInstance;
    private String mapStoragePrefix;

    private transient final ReentrantLock lock = new ReentrantLock();
    private ConcurrentHashMap<String, String> queueMaps = new ConcurrentHashMap<>();

    public BaseStorageFactory(final HazelcastInstance hazelcastInstance, int poolSize, String mapStoragePrefix) {
        this.hazelcastInstance = hazelcastInstance;
        this.mapStoragePrefix = mapStoragePrefix;

        ExecutorService executorService = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("BaseStorageFactory-" + counter++);
                return thread;
            }
        });

        for (int i = 0; i < poolSize; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        for (Map.Entry<String, String> entry : queueMaps.entrySet()) {
                            String mapName = entry.getValue();

                            IMap<UUID, BaseStorageItem> iMap = hazelcastInstance.getMap(mapName);

                            if (iMap.isEmpty()) {
                                continue;
                            }

                            Set<UUID> keys = iMap.localKeySet(new Predicates.BetweenPredicate("enqueueTime", 0l,
                                    System.currentTimeMillis()));

                            if (keys == null || keys.isEmpty()) {
                                continue;
                            }

                            String queueName = entry.getKey();
                            for (UUID key : keys) {
                                BaseStorageItem storageItem = iMap.remove(key);
                                hazelcastInstance.getQueue(queueName).add(storageItem.getObject());
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public Storage createStorage(String queueName) {

        IMap<UUID, BaseStorageItem> iMap;
        String mapName = queueMaps.get(queueName);

        if (mapName == null) {

            final ReentrantLock lock = this.lock;
            lock.lock();

            try {

                mapName = queueMaps.get(queueName);
                if (mapName == null) {
                    mapName = mapStoragePrefix + queueName;
                }
                queueMaps.put(queueName, mapName);

                iMap = hazelcastInstance.getMap(mapName);
                iMap.addIndex("enqueueTime", true);

            } finally {
                lock.unlock();
            }
        }

        iMap = hazelcastInstance.getMap(mapName);

        return new BaseStorage(iMap);
    }
}
