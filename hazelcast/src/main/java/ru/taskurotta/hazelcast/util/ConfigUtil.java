package ru.taskurotta.hazelcast.util;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class ConfigUtil {

    public static Config disableMulticast(Config config) {
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        return config;
    }

    public static Config createConfigAndDisableMulticast() {
        return disableMulticast(new Config());
    }

    public static HazelcastInstance newInstanceWithoutMulticast() {
        return Hazelcast.newHazelcastInstance(createConfigAndDisableMulticast());
    }
}
