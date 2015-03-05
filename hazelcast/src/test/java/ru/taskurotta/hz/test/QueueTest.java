package ru.taskurotta.hz.test;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.mongodb.MongoClient;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.mongodb.MongoCachedQueueStorageFactory;

import java.net.UnknownHostException;

/**
 * Created by void 17.01.14 11:20
 */
@Ignore
public class QueueTest {
    protected final static Logger log = LoggerFactory.getLogger(QueueTest.class);

    private static final int MAX_ITEMS = 100000;
    private static final String QUEUE_NAME = "testQueue";

    private MongoTemplate getMongoTemplate() throws UnknownHostException {
        return new MongoTemplate(new MongoClient("127.0.0.1"), "test");
    }

    private Config getServerConfig() throws Exception {
        Config cfg = new Config();

        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(cfg, QUEUE_NAME);
        cachedQueueConfig.setCacheSize(5);

        CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
        cachedQueueStoreConfig.setEnabled(true);
        cachedQueueStoreConfig.setBinary(false);
        cachedQueueStoreConfig.setBatchLoadSize(250);

        MongoTemplate mongoTemplate = getMongoTemplate();
        MongoCachedQueueStorageFactory mongoCachedQueueStorageFactory = new
                MongoCachedQueueStorageFactory(mongoTemplate, null);
        cachedQueueStoreConfig.setStoreImplementation(mongoCachedQueueStorageFactory.newQueueStore
                (QUEUE_NAME, cachedQueueStoreConfig));
        cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);

        return cfg;
    }

    @Test
    public void start() throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(getServerConfig());
        log.info("server instance started");
        Thread.sleep(3000);
    }

    @Test
    public void populateQueue() throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(getServerConfig());

        CachedQueue<Object> testQueue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);
        log.info("Queue size before: {}", testQueue.size());

        for (int i=0; i<MAX_ITEMS; i++) {
            testQueue.add((long) i);
        }

        log.info("Queue size after: {}", testQueue.size());
    }

    @Test
    public void queuePollTest() throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(getServerConfig());

        CachedQueue<Object> testQueue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);
        log.info("Queue size: {}", testQueue.size());
        long start = System.currentTimeMillis();

        int count = 0;
        while (testQueue.poll() != null) {
            count ++;
        }

        long time = System.currentTimeMillis() - start;
        log.info("can get {} items from queue; {} pps", count, (double)count / (double)time);
    }
}
