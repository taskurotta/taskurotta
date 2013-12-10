package ru.taskurotta.recipes.multiplier;

import java.io.Console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;

public class SpreaderTaskCreator implements Runnable, ApplicationListener<ContextRefreshedEvent> {
    private final static Logger log = LoggerFactory.getLogger(TaskCreator.class);

    private ClientServiceManager clientServiceManager;

    private int count;
    private int threadsCount = 100;

    private int multiplier = 1;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ClientServiceManager getClientServiceManager() {
        return clientServiceManager;
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void createStartTask(final MultiplierDeciderClient deciderClient) {
        log.info("warming up task launcher(s) [{}]...", threadsCount);

        log.info("launcher)s) is ready. send missile...");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            final int a = (int) (Math.random() * 100);
            final int b = (int) (Math.random() * 100);
            log.info(" Task № " + multiplier * i);
            deciderClient.multiply(a, b);
        }
        multiplier++;
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
