package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.HazelcastInstance;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:40 AM
 */
public class BaseStorageFactory implements StorageFactory {

    private HazelcastInstance hazelcastInstance;
    private String mapStoragePrefix;

    public BaseStorageFactory(HazelcastInstance hazelcastInstance, String mapStoragePrefix) {
        this.hazelcastInstance = hazelcastInstance;
        this.mapStoragePrefix = mapStoragePrefix;
    }

    @Override
    public Storage createStorage(String queueName) {
        return new BaseStorage(hazelcastInstance.getMap(mapStoragePrefix + queueName));
    }
}
