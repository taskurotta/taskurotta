package ru.taskurotta.recipes.erroneous;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;

/**
 * Created by void 27.03.13 20:22
 */
public class TaskCreator {
    private ClientServiceManager clientServiceManager;

    public void createStartTask() {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        SimpleDeciderClient decider = deciderClientProvider.getDeciderClient(SimpleDeciderClient.class);
        decider.start();
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }
}
