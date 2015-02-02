package ru.taskurotta.hazelcast.queue.config.util;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;

/**
 */
public class CachedQueueServiceConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(CachedQueueServiceConfigurator.class);

    public CachedQueueServiceConfigurator(HazelcastInstance hzInstance, int maxPercentageOfHeapSize) {

        CachedQueueServiceConfig serviceConfig = CachedQueueServiceConfig.registerServiceConfig(hzInstance.getConfig
                ());
        serviceConfig.getSizeConfig().setSize(maxPercentageOfHeapSize);
    }
}
