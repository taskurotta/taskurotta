package ru.taskurotta;

import org.junit.Test;
import ru.taskurotta.bootstrap.config.Config;
import ru.taskurotta.poc.TestRuntimeConfig;

import java.io.IOException;
import java.net.URL;

import static junit.framework.Assert.assertEquals;

/**
 * User: romario
 * Date: 2/12/13
 * Time: 12:35 PM
 */
public class TestYamlConfig {

    @Test
    public void testConfig() throws IOException {

        URL configPath = Thread.currentThread().getContextClassLoader().getResource("test-conf.yml");
        System.err.println("URL = " + configPath);
        Config config = Config.valueOf(configPath);

        assertEquals(2, ((TestRuntimeConfig) config.runtimeConfigs.get("mainRuntime")).getProperties().size());

    }

}
