package ru.taskurotta.recipes.custom;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.custom.deciders.CustomDeciderClient;

/**
 * User: stukushin
 * Date: 15.02.13
 * Time: 14:27
 */
public class TaskCreator {

    private ClientServiceManager clientServiceManager;

    private int a;
    private int b;

    public void createStartTask() {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        CustomDeciderClient customDecider = deciderClientProvider.getDeciderClient(CustomDeciderClient.class);
        customDecider.calculate(a, b);
    }

    public void setClientServiceManager(ClientServiceManager ClientServiceManager) {
        this.clientServiceManager = ClientServiceManager;
    }

    public void setA(int a) {
        this.a = a;
    }

    public void setB(int b) {
        this.b = b;
    }
}
