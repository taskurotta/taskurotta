package ru.taskurotta.backend.hz.queue.delay;

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

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:40 AM
 */
public class BaseStorageFactory implements StorageFactory {

    private HazelcastInstance hazelcastInstance;
    private String mapStoragePrefix;

    private Map<String, String> queueMaps = new ConcurrentHashMap<>();

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
                            String queueName = entry.getKey();
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
        String mapName = mapStoragePrefix + queueName;

        IMap<UUID, BaseStorageItem> iMap = hazelcastInstance.getMap(mapName);
        iMap.addIndex("enqueueTime", true);

        queueMaps.put(queueName, mapName);

        return new BaseStorage(iMap);
    }
}
