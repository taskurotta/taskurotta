package ru.taskurotta.restarter;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.restarter.deciders.CoordinatorClient;

/**
 * User: stukushin
 * Date: 05.08.13
 * Time: 11:54
 */
public class Starter {

    private ClientServiceManager clientServiceManager;

    public void start() {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        CoordinatorClient coordinator = deciderClientProvider.getDeciderClient(CoordinatorClient.class);
        coordinator.start();
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }
}
