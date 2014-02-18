package ru.taskurotta.test.fullfeature;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

import java.util.concurrent.TimeUnit;

/**
 * Created by void 20.12.13 19:05
 */
public class TaskCreator {
    private FullFeatureDeciderClient decider;

    public void startTask() {
        decider.start();
        for(int i=0; i<1000; i++) {

            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        decider = deciderClientProvider.getDeciderClient(FullFeatureDeciderClient.class);
    }
}
