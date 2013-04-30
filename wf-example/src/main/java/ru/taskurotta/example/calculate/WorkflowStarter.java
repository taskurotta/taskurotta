package ru.taskurotta.example.calculate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.example.calculate.decider.MathActionDeciderClient;
import ru.taskurotta.exception.server.ServerException;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkflowStarter {

    private ClientServiceManager clientServiceManager;

    private int count;

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStarter.class);

    public void startWork() {
        final DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        final MathActionDeciderClient decider = deciderClientProvider.getDeciderClient(MathActionDeciderClient.class);

        Thread starter = new Thread () {
            @Override
            public void run() {
                int started = 0;
                while (started < count) {
                    try{
                        decider.performAction();
                        started++;
                    } catch(ServerException ex) {
                        logger.error("Error at start new process. Strted ["+started+"] of ["+count+"]. Message: " + ex.getMessage());
                    }
                }
            }
        };

        starter.start();
        logger.info("Start work time [{}], count[{}]", new SimpleDateFormat("HH:mm:ss.SS").format(new Date()), count);
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void setCount(int count) {
        this.count = count;
    }

}
