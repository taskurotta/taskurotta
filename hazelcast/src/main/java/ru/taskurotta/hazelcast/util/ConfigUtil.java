package ru.taskurotta.hazelcast.util;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;

public class ConfigUtil {

    public static Config disableMulticast(Config config) {
        return config.setNetworkConfig(new NetworkConfig().setJoin(new JoinConfig().setMulticastConfig
                (new MulticastConfig().setEnabled(false))));
    }
}
