package ru.taskurotta.hz.test.mongo;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
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
import ru.taskurotta.hazelcast.store.MongoQueueStore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 17.02.14 12:04
 */
@Ignore
public class MongoQueueStoreTest {

    private static final Logger logger = LoggerFactory.getLogger(MongoQueueStoreTest.class);

    private MongoQueueStore mongoQueueStore;
    private MongoTemplate mongoTemplate;
    private HazelcastInstance hzInstance;
    private HzQueueConfigSupport hzQueueConfigSupport;

    private IQueue iQueue;

    public static final String QUEUE_NAME = "testMeQueue";

    @Before
    public void initCtx() {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("mongo-queue-store-test.xml");
        this.hzInstance = appContext.getBean("hzInstance", HazelcastInstance.class);
        this.hzQueueConfigSupport = appContext.getBean("hzQueueConfigSupport", HzQueueConfigSupport.class);
        this.mongoTemplate = appContext.getBean("mongoTemplate", MongoTemplate.class);
        this.mongoTemplate.getDb().dropDatabase();

        hzQueueConfigSupport.createQueueConfig(QUEUE_NAME);
        this.iQueue = hzInstance.getQueue(QUEUE_NAME);

        this.mongoQueueStore = (MongoQueueStore) hzInstance.getConfig().getQueueConfig(QUEUE_NAME).getQueueStoreConfig().getStoreImplementation();
    }


    @Test
    public void testDataLoss() {
        for (int i = 0; i < 20; i++) {
            iQueue.add("key-" + i);
        }

        long minId = mongoQueueStore.getMinItemId();

        logger.info("Min id before: " + minId);
        Assert.assertEquals(0, minId);

        this.mongoTemplate.getDb().dropDatabase();

        for (int i = 0; i < 20; i++) {
            iQueue.add("key-" + i);
        }

        try {
            Thread.sleep(2000l);//ensure mongoDB updated
        } catch (Exception e) {
            e.printStackTrace();
        }

        minId = mongoQueueStore.getMinItemId();
        logger.info("Min id after: " + minId);
        Assert.assertEquals(20, minId);
    }


    @Test
    public void testIO() throws InterruptedException {

        AtomicInteger counter = new AtomicInteger(0);

        System.err.println("Add");
        addToQueue(iQueue, counter, 19);
        TimeUnit.SECONDS.sleep(3);

        System.err.println("Poll");
        pollFromQueue(iQueue, counter, 18);
        TimeUnit.SECONDS.sleep(3);

        System.err.println("Add");
        addToQueue(iQueue, counter, 21);
        TimeUnit.SECONDS.sleep(3);

        System.err.println("Poll");
        pollFromQueue(iQueue, counter, 22);
    }

    private void addToQueue(IQueue queue, AtomicInteger counter, int quantity) {
        for (int i = 0; i < quantity; i++) {
            String item = "" + counter.getAndIncrement();

            System.err.println("add() " + item);
            iQueue.add(item);
        }
    }


    private void pollFromQueue(IQueue queue, AtomicInteger counter, int quantity) {
        for (int i = 0; i < quantity; i++) {

            String item = (String) iQueue.poll();
            System.err.println("poll() " + item);
            Assert.assertNotNull(item);
        }
    }


}
