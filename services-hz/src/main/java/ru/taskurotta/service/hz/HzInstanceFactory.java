package ru.taskurotta.service.hz;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.util.ConfigUtil;

/**
 * User: stukushin
 * Date: 04.09.2015
 * Time: 14:24
 */

public class HzInstanceFactory {

    public static HazelcastInstance createHzInstanceForTest() {
        Config config = ConfigUtil.createConfigAndDisableMulticast();
        config.setInstanceName("testInstance");
        CachedQueueServiceConfig.registerServiceConfig(config);

        SerializationConfig serializationConfig = new SerializationConfig();
        SerializerConfig serializerConfig = new SerializerConfig();
        serializerConfig.setTypeClass(TaskKey.class)
        serializationConfig.setSerializerConfigs()

        return HazelcastInstanceFactory.getOrCreateHazelcastInstance(config);
    }

}
