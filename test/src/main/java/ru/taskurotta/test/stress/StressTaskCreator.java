package ru.taskurotta.test.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.taskurotta.test.stress.LifetimeProfiler.startedProcessCounter;

/**
 * User: greg
 */
public class StressTaskCreator implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StressTaskCreator.class);
    private static final int THREADS_COUNT = 10;

    private boolean needRun = true;

    public static ExecutorService executorService;
    public static FullFeatureDeciderClient deciderClient;

    public StressTaskCreator(ClientServiceManager clientServiceManager, boolean needRun, int shotSize) {
        this.needRun = needRun;

        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        deciderClient = clientProvider.getDeciderClient(FullFeatureDeciderClient.class);

        executorService = Executors.newFixedThreadPool(THREADS_COUNT);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("onApplicationEvent");
    }


    public void sendInitialTasks(int shotSize) {
        log.info("Sending new " + shotSize + " tasks");

        sendTasks(shotSize);
    }

    public static void sendTasks(int shotSize) {

        for (int i = 0; i < shotSize; i++) {
            sendOneTask();
        }
    }

    public static void sendOneTask() {

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                boolean done = false;
                // todo: break on shutdown
                while (!done) {
                    try {
                        deciderClient.start();
                        done = true;
                        startedProcessCounter.incrementAndGet();
                    } catch (Exception e) {
                        log.warn("Start task rejected", e);
                    }
                }
            }
        });

    }

}
