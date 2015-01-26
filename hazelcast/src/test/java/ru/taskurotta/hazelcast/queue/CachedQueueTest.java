package ru.taskurotta.hazelcast.queue;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.mongodb.MongoClient;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.impl.QueueService;
import ru.taskurotta.hazelcast.queue.store.mongodb.MongoCachedQueueStorageFactory;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Ignore
public class CachedQueueTest {

    private static final Logger logger = LoggerFactory.getLogger(CachedQueueTest.class);

    private static String QUEUE_NAME = "testQueue";
    private static String MONGO_DB_NAME = "test";

    CachedQueue queue;

    @Before
    public void initCtx() throws UnknownHostException {

        Config cfg = new Config();

        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(cfg, QUEUE_NAME);
        cachedQueueConfig.setCacheSize(Integer.MAX_VALUE);

        {
            CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
            cachedQueueStoreConfig.setEnabled(true);
            cachedQueueStoreConfig.setBinary(false);
            cachedQueueStoreConfig.setBatchLoadSize(100);

            {
                MongoTemplate mongoTemplate = new MongoTemplate(new MongoClient("127.0.0.1"), MONGO_DB_NAME);
                MongoCachedQueueStorageFactory mongoCachedQueueStorageFactory = new
                        MongoCachedQueueStorageFactory(mongoTemplate);

                cachedQueueStoreConfig.setStoreImplementation(mongoCachedQueueStorageFactory.newQueueStore
                        (QUEUE_NAME, cachedQueueStoreConfig));
            }

            cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);
        }

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
        queue = hazelcastInstance.getDistributedObject(QueueService.SERVICE_NAME, QUEUE_NAME);
    }

    @Test
    public void testDataLoss() {

        String item = "testItem";

        assertNull(queue.poll());

        queue.offer(item);

        String polledItem = (String) queue.poll();

        assertNotNull(polledItem);
        assertNull(queue.poll());

    }


    @Test
    public void testMongo() {

        for (int i = 0; i < 1000; i++) {
            queue.offer(i);
        }
    }

    @Test
    public void testIO() throws InterruptedException {

        AtomicInteger counter = new AtomicInteger(0);

        System.err.println("Add");
        addToQueue(queue, counter, 19);
        TimeUnit.SECONDS.sleep(3);

        System.err.println("Poll");
        pollFromQueue(queue, counter, 18);
        TimeUnit.SECONDS.sleep(3);

        System.err.println("Add");
        addToQueue(queue, counter, 21);
        TimeUnit.SECONDS.sleep(3);

        System.err.println("Poll");
        pollFromQueue(queue, counter, 22);
    }

    private void addToQueue(CachedQueue queue, AtomicInteger counter, int quantity) {
        for (int i = 0; i < quantity; i++) {
            String item = "" + counter.getAndIncrement();

            System.err.println("add() " + item);
            queue.add(item);
        }
    }


    private void pollFromQueue(CachedQueue queue, AtomicInteger counter, int quantity) {
        for (int i = 0; i < quantity; i++) {

            String item = (String) queue.poll();
            System.err.println("poll() " + item);
            Assert.assertNotNull(item);
        }
    }


}
