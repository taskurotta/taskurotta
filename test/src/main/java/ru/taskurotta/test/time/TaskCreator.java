package ru.taskurotta.test.time;

import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskCreator {

    private ExecutorService executorService;
    private FullFeatureDeciderClient decider;

    private int threads;
    private long duration;
    private long timeout;

    private long startTime = System.currentTimeMillis();

    public void startTask() {
        for (int i = 0; i < threads; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (startTime + duration >= System.currentTimeMillis()) {
                        decider.start();

                        try {
                            TimeUnit.MILLISECONDS.sleep(timeout);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    System.exit(0);
                }
            });
        }
    }

    @Required
    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        decider = deciderClientProvider.getDeciderClient(FullFeatureDeciderClient.class);
    }

    @Required
    public void setThreads(int threads) {
        this.threads = threads;
        this.executorService = Executors.newFixedThreadPool(threads);
    }

    @Required
    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Required
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
