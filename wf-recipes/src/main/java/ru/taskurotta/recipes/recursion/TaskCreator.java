package ru.taskurotta.recipes.recursion;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.recursion.decider.FibonacciDeciderClient;

/**
 * User: stukushin
 * Date: 15.02.13
 * Time: 14:27
 */
public class TaskCreator {

    private ClientServiceManager clientServiceManager;

    private int number;
    private int count;

    public void createStartTask() {

        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        FibonacciDeciderClient fibonacciDecider = deciderClientProvider.getDeciderClient(FibonacciDeciderClient.class);

        for (int i = 0; i < count; i++) {
            fibonacciDecider.calculate(number);
        }
    }

    public void setClientServiceManager(ClientServiceManager ClientServiceManager) {
        this.clientServiceManager = ClientServiceManager;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
