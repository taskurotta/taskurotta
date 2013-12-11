package ru.taskurotta.hz.test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import ru.taskurotta.hazelcast.delay.BaseQueueFactory;
import ru.taskurotta.hazelcast.delay.BaseStorageFactory;
import ru.taskurotta.hazelcast.delay.CommonStorageFactory;
import ru.taskurotta.hazelcast.delay.DelayIQueue;
import ru.taskurotta.hazelcast.delay.QueueFactory;
import ru.taskurotta.hazelcast.delay.StorageFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DelayIQueueTest {

    @Test
    public void CommonDelayIQueueTest() throws InterruptedException {

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        try {

            StorageFactory storageFactory = new CommonStorageFactory(hazelcastInstance, "commonStorage", "");
            QueueFactory queueFactory = new BaseQueueFactory(hazelcastInstance, storageFactory);

            DelayIQueue<String> delayIQueue = queueFactory.create("testQueue");

            assertTrue(delayIQueue.add("test", 2, TimeUnit.SECONDS));

            Object retrievedObject = delayIQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            TimeUnit.SECONDS.sleep(1);

            retrievedObject = delayIQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            retrievedObject = delayIQueue.poll(1, TimeUnit.SECONDS);
            assertNotNull(retrievedObject);

            retrievedObject = delayIQueue.poll(1, TimeUnit.SECONDS);
            assertNull(retrievedObject);

        } finally {
            hazelcastInstance.shutdown();
        }
    }

    @Test
    public void BaseDelayIQueueTest() throws InterruptedException {

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        try {

            StorageFactory storageFactory = new BaseStorageFactory(hazelcastInstance, 1, "testStorage");
            QueueFactory queueFactory = new BaseQueueFactory(hazelcastInstance, storageFactory);

            DelayIQueue<String> delayIQueue = queueFactory.create("testQueue");

            assertTrue(delayIQueue.add("test", 4, TimeUnit.SECONDS));

            Object retrievedObject = delayIQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            TimeUnit.SECONDS.sleep(1);

            retrievedObject = delayIQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            retrievedObject = delayIQueue.poll(4, TimeUnit.SECONDS);
            assertNotNull(retrievedObject);

            retrievedObject = delayIQueue.poll(1, TimeUnit.SECONDS);
            assertNull(retrievedObject);

        } finally {
            hazelcastInstance.shutdown();
        }

    }
}
