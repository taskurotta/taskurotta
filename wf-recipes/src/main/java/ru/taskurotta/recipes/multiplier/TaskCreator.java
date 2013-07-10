package ru.taskurotta.recipes.multiplier;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;

/**
 * Created by void 09.07.13 19:35
 */
public class TaskCreator {
    private ClientServiceManager clientServiceManager;

    private int count;

    public void createStartTask() {
        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        MultiplierDeciderClient deciderClient = clientProvider.getDeciderClient(MultiplierDeciderClient.class);

        for (int i = 0; i < count; i++) {
            int a = (int)(Math.random() * 100);
            int b = (int)(Math.random() * 100);
            deciderClient.multiply(a, b);
        }
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
