package ru.taskurotta.server.recovery.base;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ExpirationPolicy;


/**
 * AbstractScheduledRecovery process with every recovery iteration
 * executed in several time steps for performance issues
 */
public abstract class AbstractIterableRecovery extends AbstractRecovery {

    private static final Logger logger = LoggerFactory.getLogger(AbstractIterableRecovery.class);

    protected TimeoutType timeoutType;//type of timeout to recover
    private int timeIterationStep = 10000;//period of time step iteration
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

        logger.info("[{}]: recovered [{}] tasks", getClass(), counter);

    }

    protected int processStep(int stepNumber, long timeFrom, long timeTill) {
        int counter = 0;
        CheckpointService checkpointService = getCheckpointService();

        CheckpointQuery query = new CheckpointQuery(timeoutType);
        query.setMaxTime(timeTill);
        query.setMinTime(timeFrom);

        List<Checkpoint> stepCheckpoints = checkpointService.listCheckpoints(query);

        if(stepCheckpoints!= null && !stepCheckpoints.isEmpty()) {
            for(Checkpoint checkpoint: stepCheckpoints) {
                if(isReadyToRecover(checkpoint)) {
                    try {
                        boolean success = recover(checkpoint, timeoutType);
                        checkpointService.removeCheckpoint(checkpoint);
                        if(success) {
                            counter++;
                        }
                    } catch (Exception e) {
                        logger.error("Cannot recover with checkpoint[" + checkpoint + "] and TimeoutType["+timeoutType+"]", e);
                    }
                }
            }
        }

        return counter;
    }

    protected abstract CheckpointService getCheckpointService();

    protected abstract boolean recover(Checkpoint checkpoint, TimeoutType timeoutType);

    protected boolean isReadyToRecover(Checkpoint checkpoint) {
        boolean result = false;
        if(checkpoint != null) {
            if(checkpoint.getEntityType() != null) {
                ExpirationPolicy expPolicy = getExpirationPolicy(checkpoint.getEntityType(), timeoutType);
                if(expPolicy != null) {
                    result = expPolicy.readyToRecover(checkpoint.getGuid())
                            && (System.currentTimeMillis() > expPolicy.getExpirationTime(checkpoint.getGuid(), checkpoint.getTime()));
                }
            }

        }
        return result;
    }

    public void setConfigBackend(ConfigBackend configBackend) {
        initConfigs(configBackend.getActorPreferences(), configBackend.getExpirationPolicies());//initialize expiration policies
    }
    public void setTimeoutType(TimeoutType timeoutType) {
        this.timeoutType = timeoutType;
    }
    public void setTimeIterationStep(int timeIterationStep) {
        this.timeIterationStep = timeIterationStep;
    }
    public void setTimeIterationStepUnit(TimeUnit timeIterationStepUnit) {
        this.timeIterationStepUnit = timeIterationStepUnit;
    }

}
