package ru.taskurotta.hz.test;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
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

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 08.08.13 11:15
 */
@Ignore //Requires MongoDB up and running
public class MapStoreTest {

    private static final Logger logger = LoggerFactory.getLogger(MapStoreTest.class);

    protected HazelcastInstance hzInstance;
    protected MongoTemplate mongoTemplate;

    protected static final String PURE_MAPSTORE_MAP = "pureMapStoreTest";
    protected static final String PURE_EVICTION_MAP = "pureEvictionTest";
    protected static final String MAPSTORE_EVICTION_MAP = "mapStoreWithEvictionTest";

    @Before
    public void init() {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("appContext.xml");
        hzInstance = appContext.getBean("hzInstance", HazelcastInstance.class);
        mongoTemplate = appContext.getBean("mongoTemplate", MongoTemplate.class);

        mongoTemplate.dropCollection(PURE_MAPSTORE_MAP);
        mongoTemplate.createCollection(PURE_MAPSTORE_MAP);

        mongoTemplate.dropCollection(MAPSTORE_EVICTION_MAP);
        mongoTemplate.createCollection(MAPSTORE_EVICTION_MAP);
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
        }

        logger.info("EVICTION: hzMapSize[{}]", hzMap.size());
        Assert.assertTrue(hzMap.size()== 51);//on 101 iteration 50% should be evicted and then current +1 added
    }

    @Test
    public void testEvictionWithMapStore() {
        String testMapName = MAPSTORE_EVICTION_MAP;
        IMap<String, String> hzMap = hzInstance.getMap(testMapName);

        logger.info("EVICTION WITH MAPSTORE: initial hzMap size is [{}]", hzMap.size());

        int size = 100;

        for(int i = 0; i<(size+1); i++) {
            hzMap.put("key-" + i, "val-" + i);
        }

        DBCollection mongoMap = mongoTemplate.getCollection(testMapName);
        logger.info("EVICTION WITH MAPSTORE: mongoMap size [{}], hzMapSize[{}]", mongoMap.count(), hzMap.size());

        int afterEvictionSize = size/2 + 1;
        Assert.assertTrue("Hazelcast should have "+afterEvictionSize+" values, but has: " + hzMap.size(), hzMap.size()== afterEvictionSize);//on last iteration 50% should be evicted and then current +1 added
        Assert.assertTrue("Mapstore should still contain all values", mongoMap.count() == (size+1));


        hzMap.remove("key-" + 10);
        hzMap.remove("val-" + 11);

        int newSize = size-1;
        Assert.assertTrue("Hazelcast should have "+(afterEvictionSize-2)+" values, but has: " + hzMap.size(), hzMap.size()== afterEvictionSize-2);
        Assert.assertTrue("Mapstore should still contain all values", mongoMap.count() == newSize);

    }

}
