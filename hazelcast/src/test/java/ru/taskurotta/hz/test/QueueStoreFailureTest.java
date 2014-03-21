package ru.taskurotta.hz.test;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.mongodb.DBCollection;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;

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

    protected HzQueueConfigSupport queueConfigSupport;

    public static final int COUNT = 20;

    @Before
    public void init() {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("queuestore-fail-test-mongo.xml");
        hzInstance = appContext.getBean("hzInstance", HazelcastInstance.class);
        queueConfigSupport = appContext.getBean("queueConfigSupport", HzQueueConfigSupport.class);
        mongoTemplate = appContext.getBean("mongoTemplate", MongoTemplate.class);
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
        queueConfigSupport.createQueueConfig(queueName);

        IQueue testQueue = hzInstance.getQueue(queueName);

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
        queueConfigSupport.createQueueConfig(name);
        IQueue testQueue = hzInstance.getQueue(name);
        DBCollection testQueueColl = mongoTemplate.getCollection(name);
        for (int i = 1; i <= loaded; i++) {
            testQueue.add("key-" + i);
        }

        logger.info("Hz queueSize [{}], mongo size [{}], loaded[{}]", testQueue.size(), testQueueColl.count(), loaded);

    }


}
