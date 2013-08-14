package ru.taskurotta.recipes.multiplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;

import java.io.Console;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by void 09.07.13 19:35
 */
public class TaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {
    private final static Logger log = LoggerFactory.getLogger(TaskCreator.class);

    private ClientServiceManager clientServiceManager;

    private int count;
    private int threadsCount = 100;
    private ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);

    public static final Lock Monitor = new ReentrantLock(true);
    public static final AtomicBoolean canWork = new AtomicBoolean(false);

    public void createStartTask(final MultiplierDeciderClient deciderClient) {
        log.info("warming up task launcher(s) [{}]...", threadsCount);
        Monitor.lock();

        try {
            log.info("launcher)s) is ready. send missile...");
            canWork.set(false);

            final CountDownLatch latch = new CountDownLatch(count);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
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

            System.out.printf("       Process creation rate: %8.3f pps\n",
                    1000.0D * count / (System.currentTimeMillis() - startTime));
        } finally {
            canWork.set(true);
            Monitor.unlock();
        }
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("onApplicationEvent");
        new Thread(this).start();
    }

    @Override
    public void run() {
        Console console = System.console();

        if (console != null) {
            String line = null;

            DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
            MultiplierDeciderClient deciderClient = clientProvider.getDeciderClient(MultiplierDeciderClient.class);

            System.out.println("Press fire to start...");

            while ((line = console.readLine()) != null) {
                createStartTask(deciderClient);
                System.out.println("" + count + " tasks send. Press fire to start again...");
            }
        } else {
            System.out.println("No console available!!");
        }
    }
}
