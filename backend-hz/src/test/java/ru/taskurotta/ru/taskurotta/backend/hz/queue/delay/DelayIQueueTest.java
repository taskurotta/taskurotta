package ru.taskurotta.ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.backend.hz.queue.delay.BaseQueueFactory;
import ru.taskurotta.backend.hz.queue.delay.BaseStorageFactory;
import ru.taskurotta.backend.hz.queue.delay.DelayIQueue;
import ru.taskurotta.backend.hz.queue.delay.QueueFactory;
import ru.taskurotta.backend.hz.queue.delay.StorageFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DelayIQueueTest {

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
