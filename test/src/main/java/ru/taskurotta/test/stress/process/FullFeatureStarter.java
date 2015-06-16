package ru.taskurotta.test.stress.process;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

/**
 */
public class FullFeatureStarter implements Starter {

    public static FullFeatureDeciderClient deciderClient;

    public FullFeatureStarter(ClientServiceManager clientServiceManager) {
        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        deciderClient = clientProvider.getDeciderClient(FullFeatureDeciderClient.class);
    }

    @Override
    public void start() {
        deciderClient.start();
    }
}
