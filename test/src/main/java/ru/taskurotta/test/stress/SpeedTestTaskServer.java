package ru.taskurotta.test.stress;

import com.google.common.util.concurrent.AtomicDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 5:20 PM
 */
public class SpeedTestTaskServer implements TaskServer {

    private final static Logger logger = LoggerFactory.getLogger(SpeedTestTaskServer.class);

    private TaskServer originalTaskServer;

    private static int meanLength = 1000;
    private static int reportPeriod = 5000;

    private AtomicDouble startMean = new AtomicDouble(0D);
    private AtomicDouble pollMean =  new AtomicDouble(0D);
    private AtomicDouble releaseMean = new AtomicDouble(0D);

    private AtomicInteger startCount = new AtomicInteger(0);
    private AtomicInteger pollCount = new AtomicInteger(0);
    private AtomicInteger releaseCount = new AtomicInteger(0);

    private boolean needMean = false;

    public SpeedTestTaskServer (TaskServer originalTaskServer) {

        this.originalTaskServer = originalTaskServer;

        needMean = logger.isDebugEnabled();
    }

    @Override
    public void startProcess(TaskContainer task) {

        if (!needMean) {
            originalTaskServer.startProcess(task);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            originalTaskServer.startProcess(task);
        } finally {

            long workTime = System.currentTimeMillis() - startTime;

            startMean.addAndGet((workTime - startMean.get()) / meanLength);


            if (startCount.incrementAndGet() % reportPeriod == 0) {
                logger.debug("client transport part -> startProcess {} meanTimeMls: {}", startCount,
                        (int) startMean.get());
            }

        }
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        if (!needMean) {
            return originalTaskServer.poll(actorDefinition);
        }

        long startTime = System.currentTimeMillis();

        try {
            return originalTaskServer.poll(actorDefinition);
        } finally {

            long workTime = System.currentTimeMillis() - startTime;

            pollMean.addAndGet((workTime - pollMean.get()) / meanLength);

            if (pollCount.incrementAndGet() % reportPeriod == 0) {
                logger.debug("client transport part -> poll {} meanTimeMls: {}", pollCount, (int) pollMean.get());
            }

        }
    }

    @Override
    public void release(DecisionContainer taskResult) {

        if (!needMean) {
            originalTaskServer.release(taskResult);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            originalTaskServer.release(taskResult);
        } finally {

            long workTime = System.currentTimeMillis() - startTime;

            releaseMean.addAndGet((workTime - releaseMean.get()) / meanLength);


            if (releaseCount.incrementAndGet() % reportPeriod == 0) {
                logger.debug("client transport part -> release [{}] meanTimeMls: {}", releaseCount,
                        (int) releaseMean.get());
            }

        }
    }
}
