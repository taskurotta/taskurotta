package ru.taskurotta.hz.test;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
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
 * Date: 04.02.14 18:52
 */
@Ignore //require running mongoDB
public class MapStoreFailureTest {

    private static final Logger logger = LoggerFactory.getLogger(MapStoreFailureTest.class);

    protected HazelcastInstance hzInstance;
    protected MongoTemplate mongoTemplate;

    protected String mapName = "MapStoreFailureTestMap";

    public static final int COUNT = 20;

    @Before
    public void init() {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("mapstore-fail-test-mongo.xml");
        hzInstance = appContext.getBean("hzInstance", HazelcastInstance.class);
        mongoTemplate = appContext.getBean("mongoTemplate", MongoTemplate.class);
        mongoTemplate.getDb().dropDatabase();
    }

    @Test
    public void testOnMongoStore() {

        IMap testMap = hzInstance.getMap(mapName);

        Assert.assertEquals(0, testMap.size());

        logger.info("Initial map size [{}]", testMap.size());

        for (int i = 0; i < COUNT; i++) {
            testMap.put("key-" + i, "val-" + i);
        }

        logger.info("Added [{}] values to store", COUNT);

        String failKey = "key-fail";
        String failVal = "val-fail";

        try {
            testMap.put(failKey, failVal);
        } catch (Throwable e) {
            logger.info("Exception caught [{}]", e.getLocalizedMessage());
        }

        logger.info("Map size is [{}]", testMap.size());

        Assert.assertEquals(COUNT, testMap.size());

        Object failedValue = testMap.get(failKey);
        logger.info("Failed value stored [{}], hzMap size[{}]", failedValue, testMap.size());

        Assert.assertNull(failedValue);//map should not contain failed value

    }

}
