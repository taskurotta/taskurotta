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

/**
 * User: greg
 */
public class StressTaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {

    private final static Logger log = LoggerFactory.getLogger(StressTaskCreator.class);

    private ClientServiceManager clientServiceManager;


    private static int THREADS_COUNT = 100;

    private int countOfCycles = 100;
    public static CountDownLatch LATCH;

    private ExecutorService executorService;
    private int initialSize = 5000;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("onApplicationEvent");
        Executors.newSingleThreadExecutor().submit(this);
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void createStartTask(final MultiplierDeciderClient deciderClient) {
        final CountDownLatch latch = new CountDownLatch(initialSize);

        for (int i = 0; i < initialSize; i++) {
            final int a = (int) (Math.random() * 100);
            final int b = (int) (Math.random() * 100);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    deciderClient.multiply(a, b);
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("latch.await() about process creations was interrupted", e);
        }
    }

    public int getCountOfCycles() {
        return countOfCycles;
    }

    public void setCountOfCycles(int countOfCycles) {
        this.countOfCycles = countOfCycles;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    @Override
    public void run() {
        Console console = System.console();

        if (console != null) {
            DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
            MultiplierDeciderClient deciderClient = clientProvider.getDeciderClient(MultiplierDeciderClient.class);
            System.out.println(countOfCycles + " cycle test started");
            CountDownLatch countDownLatch = new CountDownLatch(countOfCycles);
            executorService = Executors.newFixedThreadPool(THREADS_COUNT);
            while (countDownLatch.getCount() > 0) {
                LATCH = new CountDownLatch(1);
                createStartTask(deciderClient);
                try {
                    LATCH.await();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                countDownLatch.countDown();
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No console available!!");
        }
    }
}
