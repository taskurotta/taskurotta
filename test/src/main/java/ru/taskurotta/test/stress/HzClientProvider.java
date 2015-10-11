package ru.taskurotta.test.stress;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

/**
 * Created on 16.02.2015.
 */
public class HzClientProvider {

    private String addresses;

    private HazelcastInstance client;

    public HzClientProvider(String addresses) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(addresses.split(",\\s*"));
        client = HazelcastClient.newHazelcastClient(clientConfig);
    }

    public HazelcastInstance provideClient() {
        return client;
    }

}
