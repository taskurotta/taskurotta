package ru.taskurotta.bugtest.darg;

import ru.taskurotta.bugtest.darg.decider.DArgDeciderClient;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 15.07.13 17:09
 */
public class Launcher {
    private ClientServiceManager clientServiceManager;

    public void launch() {
        final DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        final DArgDeciderClient decider = deciderClientProvider.getDeciderClient(DArgDeciderClient.class);
        decider.start();
        System.out.println("Execution started");
    }


    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }
}
