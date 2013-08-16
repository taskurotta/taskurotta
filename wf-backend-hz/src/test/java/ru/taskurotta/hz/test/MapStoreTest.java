package ru.taskurotta.hz.test;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.mongodb.DBCollection;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.backend.hz.TaskKey;
import ru.taskurotta.backend.queue.TaskQueueItem;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 08.08.13 11:15
 */
//@Ignore //Requires MongoDB up and running and single node HZ
public class MapStoreTest {

    private static final Logger logger = LoggerFactory.getLogger(MapStoreTest.class);

    protected HazelcastInstance hzInstance;
    protected MongoTemplate mongoTemplate;

    protected static final String PURE_MAPSTORE_MAP = "pureMapStoreTest";
    protected static final String PURE_EVICTION_MAP = "pureEvictionTest";
    protected static final String MAPSTORE_EVICTION_MAP = "mapStoreWithEvictionTest";
    protected static final String PURE_POJO_MAPSTORE_MAP = "purePojoMapStoreTest";
    protected static final String QUEUE_MAP_NAME = "tskQueueStoreTest";

    @Before
    public void init() {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("appContext.xml");
        hzInstance = appContext.getBean("hzInstance", HazelcastInstance.class);
        mongoTemplate = appContext.getBean("mongoTemplate", MongoTemplate.class);

        mongoTemplate.dropCollection(PURE_MAPSTORE_MAP);
        mongoTemplate.createCollection(PURE_MAPSTORE_MAP);

        mongoTemplate.dropCollection(MAPSTORE_EVICTION_MAP);
        mongoTemplate.createCollection(MAPSTORE_EVICTION_MAP);

        mongoTemplate.dropCollection(PURE_POJO_MAPSTORE_MAP);
        mongoTemplate.createCollection(PURE_POJO_MAPSTORE_MAP);

        mongoTemplate.dropCollection(QUEUE_MAP_NAME);
        mongoTemplate.createCollection(QUEUE_MAP_NAME);
    }

    @Test
    public void testMapStore() {
        String testMapName = PURE_MAPSTORE_MAP;
        IMap<String, String> hzMap = hzInstance.getMap(testMapName);
        int size = 100;

        for(int i = 0; i<size; i++) {
            hzMap.put("key-" + i, "val-" + i);
        }

        DBCollection mongoMap = mongoTemplate.getCollection(testMapName);
        logger.info("MAPSTORE: mongoMap size [{}], hzMapSize[{}]", mongoMap.count(), hzMap.size());

        Assert.assertEquals("Collections in mongo and in HZ should have same size", hzMap.size(), mongoMap.count());
        Assert.assertTrue("Mongo collection should be " + size + " length", mongoMap.count() == size);

        hzMap.remove("key-" + 10);
        hzMap.remove("key-" + 11);

        Assert.assertEquals("Collections in mongo and in HZ should have same size", hzMap.size(), mongoMap.count());
        Assert.assertTrue("Mongo collection should be " + (size-2) + " length", mongoMap.count() == (size-2));

    }

    @Test
    public void testEviction() {
        String testMapName = PURE_EVICTION_MAP;
        IMap<String, String> hzMap = hzInstance.getMap(testMapName);

        for(int i = 0; i<101; i++) {
            hzMap.put("key-" + i, "val-" + i);
            try {
                Thread.sleep(1);//just in case to ensure LRU policy applied correctly
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        logger.info("EVICTION: hzMapSize[{}]", hzMap.size());
        Assert.assertTrue("Map size after eviction should be 51, but was " + hzMap.size(), hzMap.size() == 51);//on 101 iteration 50% should be evicted and then current +1 added
        Assert.assertNull("Evicted value should die without mapStore", hzMap.get("key-10"));


    }

    @Test
    public void testEvictionWithMapStore() {
        String testMapName = MAPSTORE_EVICTION_MAP;
        IMap<String, String> hzMap = hzInstance.getMap(testMapName);

        logger.info("EVICTION WITH MAPSTORE: initial hzMap size is [{}]", hzMap.size());

        int size = 100;

        for(int i = 0; i<(size+1); i++) {
            hzMap.put("key-" + i, "val-" + i);
            try {
                Thread.sleep(1);//just in case to ensure LRU policy applied correctly
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        DBCollection mongoMap = mongoTemplate.getCollection(testMapName);
        logger.info("EVICTION WITH MAPSTORE: mongoMap size [{}], hzMapSize[{}]", mongoMap.count(), hzMap.size());

        int afterEvictionSize = size/2 + 1;//half was evicted and then one added
        Assert.assertTrue("Hazelcast should have "+afterEvictionSize+" values, but has: " + hzMap.size(), hzMap.size()== afterEvictionSize);//on last iteration 50% should be evicted and then current +1 added
        Assert.assertTrue("Mapstore should still contain all "+(size+1)+" values", mongoMap.count() == (size+1));


        //81 and 82 keys exist in mongo and HZ memory
        hzMap.remove("key-" + 81);
        hzMap.remove("key-" + 82);

        int newSize = size-1;//2 values were deleted
        Assert.assertTrue("Hazelcast should have "+(afterEvictionSize-2)+" values, but has: " + hzMap.size(), hzMap.size()== afterEvictionSize-2);
        Assert.assertTrue("Mapstore should still contain all "+newSize+" values", mongoMap.count() == newSize);

        //10 and 11 keys were evicted to mongo, so they exist only there
        hzMap.remove("key-" + 10);
        hzMap.remove("key-" + 11);

        int newSize2 = size-3;//another 2 values were deleted
        Assert.assertTrue("Hazelcast should have " + (afterEvictionSize - 2) + " values, but has: " + hzMap.size(), hzMap.size() == afterEvictionSize - 2);
        Assert.assertTrue("Mapstore should still contain all " + newSize2 + " values", mongoMap.count() == newSize2);

        Assert.assertEquals("Pre-evicted Mongo value should be [val-15]", "val-15", hzMap.get("key-15"));

        //value key-15 should return to HZ memory
        Assert.assertTrue("Hazelcast should have " + (afterEvictionSize - 1) + " values, but has: " + hzMap.size(), hzMap.size() == afterEvictionSize - 1);
    }

    @Test
    public void testPojoKeyMapStore() {
        String testMapName = PURE_POJO_MAPSTORE_MAP;
        IMap<TaskKey, UUID> hzMap = hzInstance.getMap(testMapName);

        logger.info("POJO MAPSTORE: initial hzMap size is [{}]", hzMap.size());

        int size = 10;
        UUID processId = UUID.randomUUID();

        for(int i = 0; i<size; i++) {
            UUID taskId = UUID.randomUUID();
            hzMap.put(new TaskKey(processId, taskId), taskId);
        }

        DBCollection mongoMap = mongoTemplate.getCollection(testMapName);
        logger.info("EVICTION WITH POJO MAPSTORE: mongoMap size [{}], hzMapSize[{}]", mongoMap.count(), hzMap.size());

        Assert.assertEquals("Collections in mongo and in HZ should have same size", hzMap.size(), mongoMap.count());
    }

    @Test
    public void testQueueMapStore() {
        String testQueueName = QUEUE_MAP_NAME;
        String testMapName = "q:" + testQueueName;
        IQueue<TaskQueueItem> hzQueue = hzInstance.getQueue(testQueueName);
        IMap hzMap = hzInstance.getMap(testMapName);//direct link to map backing this queue
        DBCollection mongoMap = mongoTemplate.getCollection(testMapName);

        logger.info("QUEUE MAPSTORE: initial queueMap size is [{}]", hzMap.size());
        logger.info("QUEUE MAPSTORE: initial queue size is [{}]", hzQueue.size());
        logger.info("QUEUE MAPSTORE: initial mongoDB size is [{}]", mongoMap.getCount());

        int size = 100;
        UUID processId = UUID.randomUUID();

        for(int i = 0; i < size; i++) {
            hzQueue.add(getRandomTaskQueueItem(processId));
        }
        logger.info("QUEUE MAPSTORE: max queueMap size is [{}]", hzMap.size());
        logger.info("QUEUE MAPSTORE: max queue size is [{}]", hzQueue.size());
        logger.info("QUEUE MAPSTORE: mongoDB size is [{}]", mongoMap.getCount());

        hzQueue.add(getRandomTaskQueueItem(processId));//should trigger eviction

        logger.info("QUEUE MAPSTORE: queueMap size after eviction is [{}], keyset size[{}]", hzMap.size(), hzMap.keySet().size());
        logger.info("QUEUE MAPSTORE: queue size after eviction is [{}]", hzQueue.size());
        logger.info("QUEUE MAPSTORE: queueMap size after eviction(after call to hzQueue.size() ) is [{}]", hzMap.size());
        logger.info("QUEUE MAPSTORE: mongoDB size after eviction is [{}]", mongoMap.getCount());

        for(int i = 0; i < size+1; i++) {
            TaskQueueItem tqi = hzQueue.poll();
        }

        logger.info("QUEUE MAPSTORE: queueMap size after polling the queue is [{}]", hzMap.size());
        logger.info("QUEUE MAPSTORE: queue size after polling the queue is [{}]", hzQueue.size());
        logger.info("QUEUE MAPSTORE: mongoDB size after polling the queue is [{}]", mongoMap.getCount());
    }

    @Test
    public void testQueueMapStorePoll() {

    }

    private TaskQueueItem getRandomTaskQueueItem(UUID processId) {
        TaskQueueItem tqi = new TaskQueueItem();
        tqi.setStartTime(0l);
        tqi.setEnqueueTime(System.currentTimeMillis());
        tqi.setTaskList(null);
        tqi.setProcessId(processId);
        tqi.setTaskId(UUID.randomUUID());
        return tqi;
    }

}
