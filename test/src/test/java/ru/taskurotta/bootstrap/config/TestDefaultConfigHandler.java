package ru.taskurotta.bootstrap.config;

import junit.framework.Assert;
import org.junit.Test;
import ru.taskurotta.spring.configs.RuntimeConfigPathXmlApplicationContext;

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

        System.setProperty("rtCfg.sysKey1", "sysVal1");
        System.setProperty("rtCfg.key5", "value5_sys");
        String fullExternalFile = Thread.currentThread().getContextClassLoader().getResource("taskurotta/tsk-updated2.yaml").getFile();
        Config config3 = SimplifiedConfigHandler.getConfig(fullExternalFile);
        RuntimeConfigPathXmlApplicationContext rc = (RuntimeConfigPathXmlApplicationContext)(config3.runtimeConfigs.get("rtCfg"));
        Properties props = rc.getProperties();

        System.out.println("Props are: " + props);

        Assert.assertEquals("value1", props.getProperty("key1"));
        Assert.assertEquals("value2", props.getProperty("key2"));
        Assert.assertEquals("value3_upd", props.getProperty("key3"));//should be overwritten by properties file
        Assert.assertEquals("value4", props.getProperty("key4"));

        Assert.assertEquals("sysVal1", props.getProperty("sysKey1"));
        Assert.assertEquals("value5_sys", props.getProperty("key5"));//should be overwritten by system property

    }

}
