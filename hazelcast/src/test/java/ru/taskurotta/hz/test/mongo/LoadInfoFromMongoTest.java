package ru.taskurotta.hz.test.mongo;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Partition;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.mongodb.MongoCachedQueueStorageFactory;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Created by greg on 28/01/15. Check loading tail and head from MongoDB for CachedQueue
 */
@Ignore
public class LoadInfoFromMongoTest {

    private static String QUEUE_NAME = "testQueue";
    private static String MONGO_DB_NAME = "test";


    private MongoTemplate mongoTemplate;

    @Before
    public void init() throws UnknownHostException {
        mongoTemplate = new MongoTemplate(new MongoClient("127.0.0.1"), MONGO_DB_NAME);
    }

    @Test
    public void testLoad() throws InterruptedException {
        cleanAndPopulateData();
        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        CachedQueue queue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);
        Assert.assertEquals("Queue size", 15, queue.size());
        Hazelcast.shutdownAll();
    }

    @Test
    public void testQueueEmpty() throws InterruptedException {
        dataDrop();
        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        CachedQueue queue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);
        Assert.assertNull(queue.poll());
        Hazelcast.shutdownAll();

    }

    @Test
    public void testLoadInCluster() throws InterruptedException {
        cleanAndPopulateData();
        //Starting first node
        HazelcastInstance nodeOne = getHazelcastInstance();
        //Starting second node
        HazelcastInstance nodeTwo = getHazelcastInstance();
        String partitionKey = nodeOne.getPartitionService().randomPartitionKey();
        HazelcastInstance ownerInstance;
        HazelcastInstance secondInstance;
        Partition partition = nodeOne.getPartitionService().getPartition(partitionKey);
        if (nodeOne.getCluster().getLocalMember().equals(partition.getOwner())) {
            ownerInstance = nodeOne;
            secondInstance = nodeTwo;
        } else {
            ownerInstance = nodeOne;
            secondInstance = nodeTwo;
        }
        String queueNameNodeOne = QUEUE_NAME + "@" + partitionKey;
        CachedQueue<String> queueFromNodeOne = ownerInstance.getDistributedObject(CachedQueue.class.getName(), queueNameNodeOne);
        Assert.assertEquals(15, queueFromNodeOne.size());
        //polling 5 elements
        Assert.assertTrue(isOrderRight(queueFromNodeOne, 5, 0, -1));
        //shutdown first node
        ownerInstance.shutdown();
        CachedQueue<String> queueFromNodeTwo = secondInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);
        //checking size of queue
        Assert.assertEquals(9, queueFromNodeTwo.size());
        //checking order of data in queue
        Assert.assertTrue(isOrderRight(queueFromNodeTwo, 10, 6, -1));
        Hazelcast.shutdownAll();
    }

    @Test
    public void testOfferWhenLoadFromMongo() throws InterruptedException {
        cleanAndPopulateData();
        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        CachedQueue<String> queue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);
        queue.offer("testData15");
        Assert.assertEquals(true, isOrderRight(queue, 15, 0, -1));
        Hazelcast.shutdownAll();
    }

    @Test
    public void testWhenBrokenStore() throws InterruptedException {
        cleanAndPopulateData();
        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        CachedQueue<String> queue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);
        DBObject dbObject = new BasicDBObject();
        dbObject.put("_id", 9L);
        mongoTemplate.getCollection(QUEUE_NAME).remove(dbObject);
        queue.offer("testData15");
        isOrderRight(queue, 15, 0, 9L);
        Hazelcast.shutdownAll();
    }

    private boolean isOrderRight(CachedQueue<String> cachedQueue, long expectedSize, long startFrom, long skippedElement) {
        long counter = startFrom;
        while (counter <= expectedSize) {
            String val = cachedQueue.poll();
            if (counter == skippedElement) {
                counter++;
            }
            Assert.assertEquals("testData" + counter, val);
            counter++;
        }
        return true;
    }

    private void cleanAndPopulateData() {
        dataDrop();
        //adding a test data directly to mongo
        for (long i = 0; i < 15; i++) {
            DBObject dbObject = new BasicDBObject();
            dbObject.put("_id", i);
            dbObject.put("_class", "com.hazelcast.spring.mongodb.ValueWrapper");
            dbObject.put("value", "testData" + i);
            mongoTemplate.getCollection(QUEUE_NAME).insert(dbObject);

        }
    }

    private HazelcastInstance getHazelcastInstance() throws InterruptedException {
        Config cfg = new Config();

        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(cfg, QUEUE_NAME);
        cachedQueueConfig.setCacheSize(5);

        CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
        cachedQueueStoreConfig.setEnabled(true);
        cachedQueueStoreConfig.setBinary(false);
        cachedQueueStoreConfig.setBatchLoadSize(100);


        TimeUnit.SECONDS.sleep(1);
        MongoCachedQueueStorageFactory mongoCachedQueueStorageFactory = new
                MongoCachedQueueStorageFactory(mongoTemplate);

        cachedQueueStoreConfig.setStoreImplementation(mongoCachedQueueStorageFactory.newQueueStore
                (QUEUE_NAME, cachedQueueStoreConfig));

        cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);
        return Hazelcast.newHazelcastInstance(cfg);
    }

    private void dataDrop() {
        mongoTemplate.getDb().dropDatabase();
        try {
            TimeUnit.SECONDS.sleep(1);//to ensure mongoDB data flushing
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
