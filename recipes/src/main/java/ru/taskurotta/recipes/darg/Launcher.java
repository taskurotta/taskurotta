package ru.taskurotta.recipes.darg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.darg.decider.DArgDeciderClient;

/**
 * User: dimadin
 * Date: 15.07.13 17:09
 */
public class Launcher {

    private ClientServiceManager clientServiceManager;
    private static Logger logger = LoggerFactory.getLogger(Launcher.class);

    public void launch() {
        final DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        final DArgDeciderClient decider = deciderClientProvider.getDeciderClient(DArgDeciderClient.class);
        decider.start("testme!");
        logger.info("Execution started");
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }
}
