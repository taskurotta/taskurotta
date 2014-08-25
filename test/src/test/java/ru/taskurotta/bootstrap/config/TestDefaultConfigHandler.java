package ru.taskurotta.bootstrap.config;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * Created on 21.08.2014.
 */
public class TestDefaultConfigHandler {

    @Test
    public void testParseConfig() throws Exception {

        Config config1 = SimplifiedConfigHandler.getConfig(null);
        Assert.assertEquals(1, config1.actorConfigs.size());

        String externalFile = Thread.currentThread().getContextClassLoader().getResource("taskurotta/tsk-updated.yml").getFile();
        Config config2 = SimplifiedConfigHandler.getConfig(externalFile);
        Assert.assertEquals(2, config2.actorConfigs.size());


        String fullExternalFile = Thread.currentThread().getContextClassLoader().getResource("taskurotta/tsk-updated2.yaml").getFile();
        Config config3 = SimplifiedConfigHandler.getConfig(fullExternalFile);
        ru.taskurotta.spring.configs.RuntimeConfigPathXmlApplicationContext rc = (ru.taskurotta.spring.configs.RuntimeConfigPathXmlApplicationContext)(config3.runtimeConfigs.get("TestRuntimeConfig"));
        Properties props = rc.getProperties();

        Assert.assertEquals("value1", props.getProperty("key1"));
        Assert.assertEquals("value2", props.getProperty("key2"));
        Assert.assertEquals("value3_upd", props.getProperty("key3"));//should be overwritten
        Assert.assertEquals("value4", props.getProperty("key4"));
        Assert.assertEquals("value5", props.getProperty("key5"));

    }

}
