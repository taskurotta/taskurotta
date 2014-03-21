package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;

import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 09.12.13
 * Time: 18:44
 */
public class MockStorageFactory implements StorageFactory {

    public MockStorageFactory(HazelcastInstance hazelcastInstance, String mapStoragePrefix) {}

    public MockStorageFactory(HazelcastInstance hazelcastInstance, StorageFactory storageFactory) {}

    @Override
    public Storage createStorage(String queueName) {
        return new Storage() {
            @Override
            public boolean add(Object o, long delayTime, TimeUnit unit) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public void destroy() {

            }

            @Override
            public long size() {
                return 0;
            }
        };
    }
}
