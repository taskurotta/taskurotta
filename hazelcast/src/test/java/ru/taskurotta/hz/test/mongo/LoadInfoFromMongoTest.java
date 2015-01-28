package ru.taskurotta.hz.test.mongo;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
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

    private CachedQueue queue;
    private MongoTemplate mongoTemplate;

    @Before
    public void initCtx() throws UnknownHostException, InterruptedException {

        Config cfg = new Config();

        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(cfg, QUEUE_NAME);
        cachedQueueConfig.setCacheSize(5);

        {
            CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
            cachedQueueStoreConfig.setEnabled(true);
            cachedQueueStoreConfig.setBinary(false);
            cachedQueueStoreConfig.setBatchLoadSize(100);

            {
                mongoTemplate = new MongoTemplate(new MongoClient("127.0.0.1"), MONGO_DB_NAME);
                dataDrop();
                //adding a test data directly to mongo
                for (long i = 0; i < 15; i++) {
                    DBObject dbObject = new BasicDBObject();
                    dbObject.put("_id", i);
                    dbObject.put("_class", "com.hazelcast.spring.mongodb.ValueWrapper");
                    dbObject.put("value", "testData");
                    mongoTemplate.getCollection(QUEUE_NAME).insert(dbObject);

                }
                TimeUnit.SECONDS.sleep(1);
                MongoCachedQueueStorageFactory mongoCachedQueueStorageFactory = new
                        MongoCachedQueueStorageFactory(mongoTemplate);

                cachedQueueStoreConfig.setStoreImplementation(mongoCachedQueueStorageFactory.newQueueStore
                        (QUEUE_NAME, cachedQueueStoreConfig));
            }

            cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);
        }

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
        queue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);

    }


    @Test
    public void testLoad() {
        Assert.assertEquals("Queue size", 15, queue.size());
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
