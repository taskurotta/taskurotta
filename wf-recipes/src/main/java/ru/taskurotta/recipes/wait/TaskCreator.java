package ru.taskurotta.recipes.wait;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.wait.decider.WaitDeciderClient;

/**
 * Created by void 27.03.13 20:22
 */
public class TaskCreator {
    private ClientServiceManager clientServiceManager;

    public void createStartTask() {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        WaitDeciderClient waitDecider = deciderClientProvider.getDeciderClient(WaitDeciderClient.class);
        waitDecider.start();
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }
}
