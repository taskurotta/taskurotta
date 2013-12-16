package ru.taskurotta.hz.test;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import ru.taskurotta.hazelcast.queue.delay.BaseQueueFactory;
import ru.taskurotta.hazelcast.queue.delay.CommonStorageFactory;
import ru.taskurotta.hazelcast.queue.delay.DelayIQueue;
import ru.taskurotta.hazelcast.queue.delay.QueueFactory;
import ru.taskurotta.hazelcast.queue.delay.StorageFactory;
import ru.taskurotta.hazelcast.util.ConfigUtil;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DelayIQueueTest {

    @Test
    public void CommonDelayIQueueTest() throws InterruptedException {

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(ConfigUtil.disableMulticast(new Config()));

        try {

            StorageFactory storageFactory = new CommonStorageFactory(hazelcastInstance, "commonStorage", 500);
            QueueFactory queueFactory = new BaseQueueFactory(hazelcastInstance, storageFactory);

            DelayIQueue<String> delayIQueue = queueFactory.create("testQueue");

            assertTrue(delayIQueue.add("test", 2, TimeUnit.SECONDS));

            Object retrievedObject = delayIQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            TimeUnit.SECONDS.sleep(1);

            retrievedObject = delayIQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            retrievedObject = delayIQueue.poll(3, TimeUnit.SECONDS);
            assertNotNull(retrievedObject);

            retrievedObject = delayIQueue.poll(1, TimeUnit.SECONDS);
            assertNull(retrievedObject);

        } finally {
            hazelcastInstance.shutdown();
        }
    }
}
