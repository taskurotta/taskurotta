package ru.taskurotta.recipes.parallel;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.parallel.decider.PiDeciderClient;

/**
 * User: stukushin
 * Date: 15.02.13
 * Time: 14:27
 */
public class TaskCreator {

    private ClientServiceManager clientServiceManager;

    private long cycles;
    private long accuracy;

    public void createStartTask() {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        PiDeciderClient piDecider = deciderClientProvider.getDeciderClient(PiDeciderClient.class);
        piDecider.calculate(cycles, accuracy);
    }

    public void setClientServiceManager(ClientServiceManager ClientServiceManager) {
        this.clientServiceManager = ClientServiceManager;
    }

    public void setCycles(long cycles) {
        this.cycles = cycles;
    }

    public void setAccuracy(long accuracy) {
        this.accuracy = accuracy;
    }
}
