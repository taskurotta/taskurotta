package ru.taskurotta.recipes.delayed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.core.ActorSchedulingOptions;
import ru.taskurotta.internal.core.ActorSchedulingOptionsImpl;

/**
 * Created by void 09.07.13 19:35
 */
public class TaskCreator implements ApplicationListener<ContextRefreshedEvent> {
    private final static Logger log = LoggerFactory.getLogger(TaskCreator.class);

    private ClientServiceManager clientServiceManager;

    private int count;


    public void createStartTask(MultiplierDeciderClient deciderClient) {
        log.info("warming up task launcher...");
        long startTime = System.currentTimeMillis() + 1200000;
        ActorSchedulingOptions actorSchedulingOptions = new ActorSchedulingOptionsImpl();
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < 1000; j++) {
                int a = (int)(Math.random() * 100);
                int b = (int)(Math.random() * 100);
                actorSchedulingOptions.setStartTime(startTime);
                deciderClient.multiply(a, b, actorSchedulingOptions);
            }
            startTime += 1000;
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
        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        MultiplierDeciderClient deciderClient = clientProvider.getDeciderClient(MultiplierDeciderClient.class);

        createStartTask(deciderClient);
    }

}
