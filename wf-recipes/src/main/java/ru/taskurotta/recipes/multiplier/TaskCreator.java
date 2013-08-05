package ru.taskurotta.recipes.multiplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;

import java.io.Console;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by void 09.07.13 19:35
 */
public class TaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {
    private final static Logger log = LoggerFactory.getLogger(TaskCreator.class);

    private ClientServiceManager clientServiceManager;

    private int count;

    public static final Lock Monitor = new ReentrantLock(true);

    public void createStartTask(MultiplierDeciderClient deciderClient) {
        log.info("warming up task launcher...");
        Monitor.lock();
        log.info("launcher is ready. send missile...");
        try {
            for (int i = 0; i < count; i++) {
                int a = (int)(Math.random() * 100);
                int b = (int)(Math.random() * 100);
                deciderClient.multiply(a, b);
            }
        } finally {
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
                System.out.println(""+ count +" tasks send. Press fire to start again...");
            }
        } else {
            System.out.println("No console available!!");
        }
    }
}
