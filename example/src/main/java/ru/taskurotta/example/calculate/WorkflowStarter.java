package ru.taskurotta.example.calculate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.example.calculate.decider.MathActionDeciderClient;
import ru.taskurotta.exception.server.ServerException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkflowStarter {

    private ClientServiceManager clientServiceManager;

    private int count;

    private boolean startTasks = false;

    private boolean startTasksInBackground = false;

    private int startTaskPeriodSeconds = -1;

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStarter.class);

    public void startWork() {
        if (startTasks) {
            final DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
            final MathActionDeciderClient decider = deciderClientProvider.getDeciderClient(MathActionDeciderClient.class);

            if(startTaskPeriodSeconds > 0) {

                ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
                ses.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        startTasks(decider);
                    }
                }, 0, startTaskPeriodSeconds, TimeUnit.SECONDS);

            } else {
                startTasks(decider);
            }

            logger.info("Start work time [{}], count[{}]", new SimpleDateFormat("HH:mm:ss.SS").format(new Date()), count);
        }
    }


    public void startTasks(final MathActionDeciderClient decider) {
        if (startTasksInBackground) {
            startAllTasksInBackground(decider);
        } else {
            startAllTasks(decider);
        }
    }

    private void startAllTasksInBackground(final MathActionDeciderClient decider) {
        Thread starter = new Thread () {
            @Override
            public void run() {
                startAllTasks(decider);
            }
        };
        starter.start();
    }

    private void startAllTasks(MathActionDeciderClient decider) {
        int started = 0;
        while (started < count) {
            try{
                decider.performAction();
                started++;
                if (started%10 == 0) {
                    logger.info("Started [{}] processes", started);
                }
            } catch(ServerException ex) {
                logger.error("Error at start new process. Started ["+started+"] of ["+count+"]. Message: " + ex.getMessage());
            }
        }
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setStartTasks(boolean startTasks) {
        this.startTasks = startTasks;
    }

    public void setStartTasksInBackground(boolean startTasksInBackground) {
        this.startTasksInBackground = startTasksInBackground;
    }

    public void setStartTaskPeriodSeconds(int startTaskPeriodSeconds) {
        this.startTaskPeriodSeconds = startTaskPeriodSeconds;
    }
}
