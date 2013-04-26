package ru.taskurotta.example.calculate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.example.calculate.decider.MathActionDeciderClient;
import ru.taskurotta.exception.TaskurottaServerException;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkflowStarter {

    private ClientServiceManager clientServiceManager;

    private int count;

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStarter.class);

    public void startWork() {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        MathActionDeciderClient decider = deciderClientProvider.getDeciderClient(MathActionDeciderClient.class);


        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SS");
        logger.info("Start work time [{}], count[{}]", sdf.format(new Date()), count);
        int started = 0;
        while (started < count) {
            try{
                decider.performAction();
                started++;
            } catch(TaskurottaServerException ex) {
                logger.error("Error at start new process. Message: " + ex.getMessage());
            }
        }
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void setCount(int count) {
        this.count = count;
    }

}
