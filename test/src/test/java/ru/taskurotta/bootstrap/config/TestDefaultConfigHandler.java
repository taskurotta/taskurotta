package ru.taskurotta.bootstrap.config;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created on 21.08.2014.
 */
public class TestDefaultConfigHandler {

    @Test
    public void testParseConfig() throws Exception {

        Config config1 = DefaultConfigHandler.getConfig(null);
        Assert.assertEquals(1, config1.actorConfigs.size());

        String externalFile = Thread.currentThread().getContextClassLoader().getResource("taskurotta/tsk-updated.yml").getFile();
        Config config2 = DefaultConfigHandler.getConfig(externalFile);
        Assert.assertEquals(2, config2.actorConfigs.size());

    }

}
