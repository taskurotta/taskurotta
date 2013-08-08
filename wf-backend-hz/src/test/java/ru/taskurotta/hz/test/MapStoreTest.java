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

    @Before
    public void init() {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("appContext.xml");
        hzInstance = appContext.getBean("hzInstance", HazelcastInstance.class);
        mongoTemplate = appContext.getBean("mongoTemplate", MongoTemplate.class);
    }

    @Test
    public void testMapStore() {
        String testMapName = "pureMapStoreTest";
        IMap<String, String> hzMap = hzInstance.getMap(testMapName);

        for(int i = 0; i<100; i++) {
            hzMap.put("storeKey-" + i, "storeValue-" + i);
        }

        DBCollection mongoMap = mongoTemplate.getCollection(testMapName);
        logger.info("MAPSTORE: mongoMap size [{}], hzMapSize[{}]", mongoMap.count(), hzMap.size());
        Assert.assertEquals(hzMap.size(), mongoMap.count());

    }

    @Test
    public void testEviction() {
        String testMapName = "pureEvictionTest";
        IMap<String, String> hzMap = hzInstance.getMap(testMapName);

        for(int i = 0; i<101; i++) {
            hzMap.put("storeKey-" + i, "storeValue-" + i);
        }

        logger.info("EVICTION: hzMapSize[{}]", hzMap.size());
        Assert.assertTrue(hzMap.size()== 51);//on 101 iteration 50% should be evicted and then current +1 added
    }

    @Test
    public void testEvictionWithMapStore() {
        String testMapName = "mapStoreWithEvictionTest";
        IMap<String, String> hzMap = hzInstance.getMap(testMapName);

        for(int i = 0; i<101; i++) {
            hzMap.put("evictKey-" + i, "evictValue-" + i);
        }

        DBCollection mongoMap = mongoTemplate.getCollection(testMapName);
        logger.info("EVICTION WITH MAPSTORE: mongoMap size [{}], hzMapSize[{}]", mongoMap.count(), hzMap.size());

        Assert.assertTrue("Eviction doesnt works", hzMap.size()== 51);//on 101 iteration 50% should be evicted and then current +1 added
        Assert.assertTrue("Mapstore doesnt works", mongoMap.count() == 101);

    }

}
