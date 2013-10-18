package ru.taskurotta.recipes.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.multiplier.MultiplierDeciderClient;

import java.io.Console;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: greg
 */
public class StressTaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {

    private final static Logger log = LoggerFactory.getLogger(StressTaskCreator.class);

    private ClientServiceManager clientServiceManager;


    private static int THREADS_COUNT = 50;

    private int countOfCycles = 100;
    private boolean needRun = true;
    public static CountDownLatch LATCH;
    private ExecutorService executorService;
    private static int shotSize = 2000;
    private static AtomicInteger currentCycle = new AtomicInteger(0);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("onApplicationEvent");
        if (needRun) {
            Executors.newSingleThreadExecutor().submit(this);
        }
    }

    public void setNeedRun(boolean needRun) {
        this.needRun = needRun;
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void createStartTask(final MultiplierDeciderClient deciderClient) {

        for (int i = 0; i < shotSize; i++) {
            final int a = (int) (Math.random() * 100);
            final int b = (int) (Math.random() * 100);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    deciderClient.multiply(a, b);
                }
            });
        }

    }

    public int getCountOfCycles() {
        return countOfCycles;
    }

    public void setCountOfCycles(int countOfCycles) {
        this.countOfCycles = countOfCycles;
    }

    public static int getShotSize() {
        return shotSize;
    }

    @Override
    public void run() {
        Console console = System.console();
        if (console != null) {
            DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
            MultiplierDeciderClient deciderClient = clientProvider.getDeciderClient(MultiplierDeciderClient.class);
            executorService = Executors.newFixedThreadPool(THREADS_COUNT);
            for (int i = 0; i < 3; i++) {
                createStartTask(deciderClient);
            }
            while (LifetimeProfiler.stabilizationCounter.get() < 10) {
                LATCH = new CountDownLatch(1);
                createStartTask(deciderClient);
                try {
                    LATCH.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long deltaTime = LifetimeProfiler.lastTime.get() - LifetimeProfiler.startTime.get();
            double time = 1.0 * deltaTime / 1000.0;
            long meanTaskCount = LifetimeProfiler.taskCount.get();
            double rate = 1000.0 * meanTaskCount / deltaTime;
            double totalDelta = LifetimeProfiler.totalDelta / (meanTaskCount / LifetimeProfiler.tasksForStat);
            System.out.println("Total task count: " + LifetimeProfiler.taskCount);
            System.out.println("Delta time: " + deltaTime);
            System.out.printf("TOTAL: tasks: %6d; time: %6.3f s; rate: %8.3f tps; totalDelta: %8.3f \n", meanTaskCount, time, rate, totalDelta);
            System.out.println("End");
            System.exit(0);
        } else {
            System.out.println("No console available!!");
        }
    }
}
