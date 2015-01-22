package ru.taskurotta.test.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.taskurotta.test.stress.LifetimeProfiler.lastTime;
import static ru.taskurotta.test.stress.LifetimeProfiler.stabilizationCounter;
import static ru.taskurotta.test.stress.LifetimeProfiler.startTime;
import static ru.taskurotta.test.stress.LifetimeProfiler.startedProcessCounter;
import static ru.taskurotta.test.stress.LifetimeProfiler.stopDecorating;
import static ru.taskurotta.test.stress.LifetimeProfiler.taskCount;
import static ru.taskurotta.test.stress.LifetimeProfiler.tasksForStat;

/**
 * User: greg
 */
public class StressTaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StressTaskCreator.class);
    private static final int THREADS_COUNT = 50;

    private boolean needRun = true;
    public static CountDownLatch cd;

    public static ExecutorService executorService;
    public static FullFeatureDeciderClient deciderClient;

    public StressTaskCreator(ClientServiceManager clientServiceManager, boolean needRun, int shotSize) {
        this.needRun = needRun;

        cd = new CountDownLatch(shotSize);

        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        deciderClient = clientProvider.getDeciderClient(FullFeatureDeciderClient.class);

        executorService = Executors.newFixedThreadPool(THREADS_COUNT);
        sendInitialTasks(shotSize);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("onApplicationEvent");
        if (needRun) {
            final ExecutorService executorService1 = Executors.newSingleThreadExecutor();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    executorService1.shutdown();
                }
            });

            executorService1.submit(this);
        }
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
                        cd.countDown();
                        done = true;
                        startedProcessCounter.incrementAndGet();
                    } catch (Exception e) {
                        log.warn("Start task rejected", e);
                    }
                }
            }
        });

    }


    @Override
    public void run() {

        while (stabilizationCounter.get() < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long deltaTime = lastTime.get() - startTime.get();
        double time = 1.0 * deltaTime / 1000.0;
        long meanTaskCount = taskCount.get();
        double rate = 1000.0 * meanTaskCount / deltaTime;
        double totalDelta = LifetimeProfiler.totalDelta / (meanTaskCount / tasksForStat);

        log.info("Total task count: " + taskCount);
        log.info("Delta time: " + deltaTime);
        log.info(String.format("TOTAL: tasks: %6d; time: %6.3f s; rate: %8.3f tps; totalDelta: %8.3f \n", meanTaskCount, time, rate, totalDelta));
        stopDecorating.set(true);
        log.info("Decoration stopped");
        log.info("End");

        System.exit(0);
    }

}
