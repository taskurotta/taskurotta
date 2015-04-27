package ru.taskurotta.hazelcast.queue.config.util;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;

public class CachedQueueServiceConfigurator {

    public CachedQueueServiceConfigurator(HazelcastInstance hzInstance, int maxPercentageOfHeapSize) {
        CachedQueueServiceConfig serviceConfig = CachedQueueServiceConfig.registerServiceConfig(hzInstance.getConfig());
        serviceConfig.getSizeConfig().setSize(maxPercentageOfHeapSize);
    }

}
