package ru.taskurotta.ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.backend.hz.queue.delay.BaseQueueFactory;
import ru.taskurotta.backend.hz.queue.delay.BaseStorageFactory;
import ru.taskurotta.backend.hz.queue.delay.CommonStorageFactory;
import ru.taskurotta.backend.hz.queue.delay.DelayIQueue;
import ru.taskurotta.backend.hz.queue.delay.QueueFactory;
import ru.taskurotta.backend.hz.queue.delay.StorageFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DelayIQueueTest {

    @Test
    public void CommonDelayIQueueTest() throws InterruptedException {

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        try {

            StorageFactory storageFactory = new CommonStorageFactory(hazelcastInstance, "commonDelayStorageName");
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
    @Ignore
    public void BaseDelayIQueueTest() throws InterruptedException {

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        try {

            StorageFactory storageFactory = new BaseStorageFactory(hazelcastInstance, "dqs#");
            QueueFactory queueFactory = new BaseQueueFactory(hazelcastInstance, storageFactory);

            DelayIQueue delayIQueue = queueFactory.create("testQueue");

            delayIQueue.add(new Object(), 500, TimeUnit.MICROSECONDS);

            Object retrievedObject = delayIQueue.poll(0, TimeUnit.SECONDS);
            assertNull(retrievedObject);

            Thread.sleep(1000);

            retrievedObject = delayIQueue.poll(0, TimeUnit.SECONDS);
            assertNotNull(retrievedObject);

        } finally {
            hazelcastInstance.shutdown();
        }

    }
}
