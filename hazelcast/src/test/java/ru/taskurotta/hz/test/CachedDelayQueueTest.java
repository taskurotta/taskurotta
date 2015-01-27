package ru.taskurotta.hz.test;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.delay.CachedDelayQueue;
import ru.taskurotta.hazelcast.queue.delay.DefaultQueueFactory;
import ru.taskurotta.hazelcast.queue.delay.DefaultStorageFactory;
import ru.taskurotta.hazelcast.queue.delay.QueueFactory;
import ru.taskurotta.hazelcast.queue.delay.StorageFactory;
import ru.taskurotta.hazelcast.util.ConfigUtil;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CachedDelayQueueTest {

    @Test
    public void CommonDelayIQueueTest() throws InterruptedException {

        Config config = ConfigUtil.disableMulticast(new Config());
        CachedQueueServiceConfig.registerServiceConfig(config);

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        try {

            StorageFactory storageFactory = new DefaultStorageFactory(hazelcastInstance, "commonStorage", 500);
            QueueFactory queueFactory = new DefaultQueueFactory(hazelcastInstance, storageFactory);

            CachedDelayQueue<String> cachedDelayQueue = queueFactory.create("testQueue");

            assertTrue(cachedDelayQueue.delayOffer("test", 2, TimeUnit.SECONDS));

            Object retrievedObject = cachedDelayQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            TimeUnit.SECONDS.sleep(1);

            retrievedObject = cachedDelayQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            retrievedObject = cachedDelayQueue.poll(3, TimeUnit.SECONDS);
            assertNotNull(retrievedObject);

            retrievedObject = cachedDelayQueue.poll(1, TimeUnit.SECONDS);
            assertNull(retrievedObject);

        } finally {
            hazelcastInstance.shutdown();
        }
    }
}
