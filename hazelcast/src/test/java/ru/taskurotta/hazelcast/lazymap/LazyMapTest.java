package ru.taskurotta.hazelcast.lazymap;

import com.hazelcast.config.Config;
import com.hazelcast.config.ServiceConfig;
import com.hazelcast.config.ServicesConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import ru.taskurotta.hazelcast.util.ConfigUtil;

/**
 */
public class LazyMapTest {

    @Test
    public void init() {
        Config config = new Config();

        ServicesConfig lazyMapConfig = config.getServicesConfig();

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setEnabled(true);
        serviceConfig.setName(ProxyImpl.SRV_NAME);
        serviceConfig.setServiceImpl(new Service());

        lazyMapConfig.addServiceConfig(serviceConfig);

        ConfigUtil.disableMulticast(config);

        HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);

        LazyMap lMap = (LazyMap) hz.getDistributedObject(LazyMap.SRV_NAME, "test");
    }
}
