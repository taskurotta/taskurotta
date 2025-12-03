package ru.taskurotta.recipes.idempotencykey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.core.TaskConfig;

import java.util.UUID;

/**
 * Created by void 09.07.13 19:35
 */
public class TaskCreator implements ApplicationListener<ContextRefreshedEvent> {
    private final static Logger log = LoggerFactory.getLogger(TaskCreator.class);

    private ClientServiceManager clientServiceManager;

    private int count;


    public void createStartTask(MultiplierDeciderClient deciderClient) {
        log.info("Start {} processes", count);

        String key = UUID.randomUUID().toString();
        TaskConfig taskConfig = new TaskConfig().setIdempotencyKey(key);
        for (int i = 0; i < count; i++) {
            deciderClient.multiply(10, 10, taskConfig);
        }

        log.info("{} processes started", count);
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
        ru.taskurotta.recipes.idempotencykey.MultiplierDeciderClient deciderClient = clientProvider.getDeciderClient(ru.taskurotta.recipes.idempotencykey.MultiplierDeciderClient.class);

        createStartTask(deciderClient);
    }

}
