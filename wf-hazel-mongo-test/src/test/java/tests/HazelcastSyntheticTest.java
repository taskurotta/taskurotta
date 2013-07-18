package tests;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.StopWatch;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.UUID;

/**
 * User: greg
 */
@ContextConfiguration(locations = {"classpath:context.xml"})
public class HazelcastSyntheticTest extends AbstractTestNGSpringContextTests {

    @Autowired
    @Qualifier("hzInstance")
    HazelcastInstance hazelcastInstance;

    //@Test
    public void syntheticTest() {
        IMap<String, UUID> map = hazelcastInstance.getMap("id2TaskMap");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < 10000; i++) {
            map.put("Test" + i, UUID.randomUUID());
        }
        stopWatch.stop();
        System.out.println(stopWatch.shortSummary());
        for (int i = 0; i < 10000; i++) {
            Assert.assertTrue(map.containsKey("Test" + i));
            Assert.assertNotNull(map.get("Test" + i));
        }

    }
}
