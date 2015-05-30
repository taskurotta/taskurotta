package ru.taskurotta.test.stress;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.util.StringUtils;

/**
 * Created on 16.02.2015.
 */
public class HzClientProvider {

    private HazelcastInstance client;

    public HzClientProvider(String addresses) {
        if (StringUtils.hasText(addresses)) {
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.getNetworkConfig().addAddress(addresses.split(",\\s*"));
            client = HazelcastClient.newHazelcastClient(clientConfig);
        } else {
            client = null;
        }
    }

    public HazelcastInstance provideClient() {
        return client;
    }

}
