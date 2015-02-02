package com.hazelcast.queue;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastTestSupport;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.impl.MockCachedQueueStore;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;

public abstract class AbstractQueueTest extends HazelcastTestSupport {



    protected CachedQueue newCachedQueue() {
        final Config cfg = new Config();
        final String QUEUE_NAME = randomString();
        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(cfg, QUEUE_NAME);
        cachedQueueConfig.setCacheSize(5);
        CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
        cachedQueueStoreConfig.setEnabled(true);
        cachedQueueStoreConfig.setBinary(false);
        cachedQueueStoreConfig.setBatchLoadSize(100);
        CachedQueueStore store = new MockCachedQueueStore();
        cachedQueueStoreConfig.setStoreImplementation(store);
        cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);
        HazelcastInstance instance = createHazelcastInstance(cfg);
        return instance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);
    }


}