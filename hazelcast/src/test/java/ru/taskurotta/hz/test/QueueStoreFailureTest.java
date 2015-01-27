package ru.taskurotta.hz.test;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.mongodb.MongoCachedQueueStorageFactory;

import java.net.UnknownHostException;

/**
 * Javadoc should be here
 * Date: 12.02.14 15:57
 */
@Ignore //require mongo DB running
public class QueueStoreFailureTest {

    private static final Logger logger = LoggerFactory.getLogger(QueueStoreFailureTest.class);

    protected HazelcastInstance hzInstance;
    protected MongoTemplate mongoTemplate;

    protected String queueName = "QueueStoreFailure";

    private static String MONGO_DB_NAME = "test";

    protected HzQueueConfigSupport queueConfigSupport;

    public static final int COUNT = 20;

    @Before
    public void init() throws UnknownHostException {


        Config cfg = new Config();

        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(cfg, queueName);
        cachedQueueConfig.setCacheSize(5);

        CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
        cachedQueueStoreConfig.setEnabled(true);
        cachedQueueStoreConfig.setBinary(false);
        cachedQueueStoreConfig.setBatchLoadSize(250);

        mongoTemplate = new MongoTemplate(new MongoClient("127.0.0.1"), MONGO_DB_NAME);
        MongoCachedQueueStorageFactory mongoCachedQueueStorageFactory = new
                MongoCachedQueueStorageFactory(mongoTemplate);

        cachedQueueStoreConfig.setStoreImplementation(mongoCachedQueueStorageFactory.newQueueStore
                (queueName, cachedQueueStoreConfig));
        cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);

        hzInstance = Hazelcast.newHazelcastInstance(cfg);
        dataDrop();
    }


    private void dataDrop() {
        mongoTemplate.getDb().dropDatabase();

        try {
            Thread.sleep(1000);//to ensure mongoDB data flushing
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOnMongoStore() {

        CachedQueue testQueue = hzInstance.getDistributedObject(CachedQueue.class.getName(), queueName);

        Assert.assertEquals(0, testQueue.size());
        logger.info("Init: hz queueSize [{}], mongo size [{}]", testQueue.size(), mongoTemplate.getCollection(queueName).count());

        for (int i = 0; i < COUNT; i++) {
            testQueue.add("key-" + i);
        }

        logger.info("Before drop: hz queueSize [{}], mongo size [{}]", testQueue.size(), mongoTemplate.getCollection(queueName).count());

        dataDrop();

        logger.info("After drop: hz queueSize [{}], mongo size [{}]", testQueue.size(), mongoTemplate.getCollection(queueName).count());

        for (int i = 0; i < COUNT; i++) {
            Object item = testQueue.poll();
            logger.info("Polled object[{}], queue size [{}]", item, testQueue.size());

        }

        Assert.assertEquals(0, testQueue.size());

    }

    @Test
    public void testQueueStoreCons() {
        String name = "yetAnotherQueue";
        int loaded = 1000;
//        queueConfigSupport.createQueueConfig(name);
        CachedQueue testQueue = hzInstance.getDistributedObject(CachedQueue.class.getName(), name);
        DBCollection testQueueColl = mongoTemplate.getCollection(name);
        for (int i = 1; i <= loaded; i++) {
            testQueue.add("key-" + i);
        }

        logger.info("Hz queueSize [{}], mongo size [{}], loaded[{}]", testQueue.size(), testQueueColl.count(), loaded);

    }


}
