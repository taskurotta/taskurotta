package ru.taskurotta.recipes.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.multiplier.MultiplierDeciderClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.taskurotta.recipes.stress.LifetimeProfiler.lastTime;
import static ru.taskurotta.recipes.stress.LifetimeProfiler.stabilizationCounter;
import static ru.taskurotta.recipes.stress.LifetimeProfiler.startTime;
import static ru.taskurotta.recipes.stress.LifetimeProfiler.stopDecorating;
import static ru.taskurotta.recipes.stress.LifetimeProfiler.taskCount;
import static ru.taskurotta.recipes.stress.LifetimeProfiler.tasksForStat;

/**
 * User: greg
 */
public class StressTaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {

    private final static Logger log = LoggerFactory.getLogger(StressTaskCreator.class);

    private ClientServiceManager clientServiceManager;


    private int THREADS_COUNT = 50;
    private int initialCount;
    private boolean needRun = true;
    public static CountDownLatch cd;

    public static AtomicInteger taskCompletedCounter = new AtomicInteger(0);
    public static ExecutorService executorService;
    public static MultiplierDeciderClient deciderClient;
    private static int shotSize;

    public StressTaskCreator(ClientServiceManager clientServiceManager, boolean needRun, int shotSize, int initialCount) {
        this.clientServiceManager = clientServiceManager;
        this.needRun = needRun;
        this.initialCount = initialCount;
        this.shotSize = shotSize;

        this.cd = new CountDownLatch(shotSize * initialCount);


        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        deciderClient = clientProvider.getDeciderClient(MultiplierDeciderClient.class);

        executorService = Executors.newFixedThreadPool(THREADS_COUNT);
        for (int i = 0; i < initialCount; i++) {
            sendInitialTasks();
        }

    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("onApplicationEvent");
        if (needRun) {
            Executors.newSingleThreadExecutor().submit(this);
        }
    }


    public void sendInitialTasks() {

        log.info("Sending new " + shotSize + " tasks...");

        long ctm = System.currentTimeMillis();

        ThreadLocalRandom tlr = ThreadLocalRandom.current();

        final int a = tlr.nextInt(100);
        final int b = tlr.nextInt(100);

        for (int i = 0; i < shotSize; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    deciderClient.multiply(a, b);
                    cd.countDown();
                }
            });
        }

    }

    public static void sendOneTask(ThreadLocalRandom tlr) {

        final int a = tlr.nextInt(100);
        final int b = tlr.nextInt(100);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                deciderClient.multiply(a, b);
            }
        });

    }


    @Override
    public void run() {

        while (stabilizationCounter.get() < 10) {
//            LATCH = new CountDownLatch(1);
//            sendInitialTasks(deciderClient);
            try {
//                LATCH.await();
                TimeUnit.MILLISECONDS.sleep(1000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

//            while (taskCompletedCounter.decrementAndGet() > 0) {
//                sendOneTask(tlr);
//            }
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
