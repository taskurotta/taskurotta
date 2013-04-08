package ru.taskurotta.bootstrap.config;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 17:53
 */
public class ConfigTest {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testValueOfFile() throws Exception {
        File configFile = new File(Thread.currentThread().getContextClassLoader().getResource("test-conf.yml").getFile());
        Config config = Config.valueOf(configFile);

        assertEquals(1, config.runtimeConfigs.size());
        assertEquals(1, config.spreaderConfigs.size());
        assertEquals(1, config.profilerConfigs.size());
        assertEquals(1, config.actorConfigs.size());
        assertEquals(1, config.policyConfigs.size());
    }

    @Test
    public void testValueOfResource() throws Exception {
        URL configURL = Thread.currentThread().getContextClassLoader().getResource("test-conf.yml");
        Config config = Config.valueOf(configURL);

        assertEquals(1, config.runtimeConfigs.size());
        assertEquals(1, config.spreaderConfigs.size());
        assertEquals(1, config.profilerConfigs.size());
        assertEquals(1, config.actorConfigs.size());
        assertEquals(1, config.policyConfigs.size());
    }
}
