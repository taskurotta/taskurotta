package ru.taskurotta.server.recovery.base;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * AbstractScheduledRecovery process with every recovery iteration
 * executed in several time steps for performance issues
 */
public abstract class AbstractScheduledIterableRecovery extends AbstractScheduledRecovery {

    private static final Logger logger = LoggerFactory.getLogger(AbstractScheduledIterableRecovery.class);

    private int timeIterationStep = 10000;
    private TimeUnit timeIterationStepUnit = TimeUnit.MILLISECONDS;

    protected void processRecoveryIteration() {
        int counter = 0;
        int step = 0;
        long timeFrom = System.currentTimeMillis() - recoveryPeriodUnit.toMillis(recoveryPeriod);
        while(timeFrom < System.currentTimeMillis()) {
            step++;
            long timeTill =  timeFrom+timeIterationStepUnit.toMillis(timeIterationStep);
            counter += processStep(step, timeFrom, timeTill);
            timeFrom = timeTill;
        }

        logger.info("Recovery [{}]: recovered [{}] tasks", getClass(), counter);

    }

    protected abstract int processStep(int stepNumber, long timeFrom, long timeTill);

    public void setTimeIterationStep(int timeIterationStep) {
        this.timeIterationStep = timeIterationStep;
    }

    public void setTimeIterationStepUnit(TimeUnit timeIterationStepUnit) {
        this.timeIterationStepUnit = timeIterationStepUnit;
    }

}
