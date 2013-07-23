package ru.taskurotta.recipes.pcollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.pcollection.decider.CollectionOfPromiseDeciderClient;

/**
 * User: dimadin
 * Date: 22.07.13 11:15
 */
public class Launcher {

    private ClientServiceManager clientServiceManager;
    private static Logger logger = LoggerFactory.getLogger(Launcher.class);

    private int size = 2;

    public void launch() {
        final DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        final CollectionOfPromiseDeciderClient decider = deciderClientProvider.getDeciderClient(CollectionOfPromiseDeciderClient.class);
        decider.execute(size);
        logger.info("Execution started");
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
