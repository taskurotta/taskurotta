package ru.taskurotta.service.hz;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;

import java.io.IOException;
import java.io.InputStream;

/**
 * User: stukushin
 * Date: 04.09.2015
 * Time: 14:24
 */

public class HzInstanceFactory {

    public static HazelcastInstance createHzInstanceForTest() {
        Config config;
        try (InputStream inputStream = HzInstanceFactory.class.getClassLoader().getResourceAsStream("spring/hz-test.xml")) {
            config = new XmlConfigBuilder(inputStream).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        config.setInstanceName("testInstance");
        CachedQueueServiceConfig.registerServiceConfig(config);

        return HazelcastInstanceFactory.getOrCreateHazelcastInstance(config);
    }

}
