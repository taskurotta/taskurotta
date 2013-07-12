package ru.taskurotta.recipes.retrypolicy;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.retrypolicy.decider.SumDeciderClient;

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
        SumDeciderClient sumDecider = deciderClientProvider.getDeciderClient(SumDeciderClient.class);
        sumDecider.calculate(a, b);
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
