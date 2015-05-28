package ru.taskurotta.hazelcast.config;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ManagedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

/**
 */
public class HazelcastInstanceFactory {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastInstanceFactory.class);

    public static HazelcastInstance create(String propertiesFileName, String hazelcastConfigFileName,
                                           Collection<MapConfig> mapConfigs,
                                           Collection<CachedQueueConfig> cachedQueueConfigs,
                                           ManagedContext managedContext,
                                           int cachedQueueMaxPercentageOfHeapSize,
                                           Properties properties) throws IOException {

//        Properties properties = new Properties();
//        properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFileName));

        Config config = null;

        if (properties != null) {
            config = new ClasspathXmlConfig(hazelcastConfigFileName, properties);
        } else {
            config = new ClasspathXmlConfig(hazelcastConfigFileName);
        }

        if (mapConfigs != null) {
            for (MapConfig mapConfig : mapConfigs) {
                config.addMapConfig(mapConfig);
            }
        }

        CachedQueueServiceConfig cachedQueueServiceConfig = CachedQueueServiceConfig.registerServiceConfig(config);

        if (cachedQueueMaxPercentageOfHeapSize != -1) {
            cachedQueueServiceConfig.getSizeConfig().setSize(cachedQueueMaxPercentageOfHeapSize);
        }

        if (cachedQueueConfigs != null) {
            for (CachedQueueConfig cachedQueueConfig : cachedQueueConfigs) {
                cachedQueueServiceConfig.addQueueConfig(cachedQueueConfig);
            }
        }

        if (managedContext != null) {
            config.setManagedContext(managedContext);
        }

        logger.debug("Hazelcast config is: " + config);

        return Hazelcast.newHazelcastInstance(config);
    }
}
