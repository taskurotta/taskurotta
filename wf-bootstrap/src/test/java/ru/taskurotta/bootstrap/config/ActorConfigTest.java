package ru.taskurotta.bootstrap.config;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 18:53
 */
public class ActorConfigTest {

     private static ActorConfig actorConfig;

    @BeforeClass
    public static void setUp() throws Exception {
        URL configURL = Thread.currentThread().getContextClassLoader().getResource("test-conf.yml");
        Config config = Config.valueOf(configURL);
        actorConfig = config.actorConfigs.get(0);
    }

    @Test
    public void testGetActorInterface() throws Exception {
        assertEquals("ru.taskurotta.bootstrap.TestWorker", actorConfig.getActorInterface());
    }

    @Test
    public void testGetRuntimeConfig() throws Exception {
        assertEquals("TestRuntimeConfig", actorConfig.getRuntimeConfig());
    }

    @Test
    public void testGetSpreaderConfig() throws Exception {
        assertEquals("TestTaskSpreaderConfig", actorConfig.getSpreaderConfig());
    }

    @Test
    public void testGetProfilerConfig() throws Exception {
        assertEquals("TestProfilerConfig", actorConfig.getProfilerConfig());
    }

    @Test
    public void testGetCount() throws Exception {
        assertEquals(10, actorConfig.getCount());
    }
}
