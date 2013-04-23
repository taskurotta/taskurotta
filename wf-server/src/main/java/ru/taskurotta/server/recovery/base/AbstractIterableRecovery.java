package ru.taskurotta.server.recovery.base;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ExpirationPolicy;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * AbstractScheduledRecovery process with every recovery iteration
 * executed in several time steps for performance issues
 */
public abstract class AbstractIterableRecovery extends AbstractRecovery {

    private int timeIterationStep = 10000;//period of time step iteration
    private TimeUnit timeIterationStepUnit = TimeUnit.MILLISECONDS;

    protected void processRecoveryIteration() {
        long start = System.currentTimeMillis();
        int counter = 0;
        int step = 0;
        long timeFrom = System.currentTimeMillis() - recoveryPeriodUnit.toMillis(recoveryPeriod);
        while(timeFrom < System.currentTimeMillis()) {
            step++;
            long timeTill =  timeFrom+timeIterationStepUnit.toMillis(timeIterationStep);
            counter += processStep(step, timeFrom, timeTill);
            timeFrom = timeTill;
        }

        logger.info("Recovered [{}] tasks in [{}]ms", counter, (System.currentTimeMillis()-start));

    }

    protected abstract TimeoutType[] getSupportedTimeouts();

    protected int processStep(int stepNumber, long timeFrom, long timeTill) {
        int counter = 0;
        TimeoutType[] timeouts = getSupportedTimeouts();//type of timeout to recover
        if(timeouts!=null && timeouts.length>0) {
            for(TimeoutType timeoutType: timeouts) {//TODO: specify timeouts collection in CheckpointQuery
                CheckpointQuery query = new CheckpointQuery(timeoutType);
                query.setMaxTime(timeTill);
                query.setMinTime(timeFrom);

                List<Checkpoint> stepCheckpoints = checkpointService.listCheckpoints(query);

                if(stepCheckpoints!= null && !stepCheckpoints.isEmpty()) {
                    for(Checkpoint checkpoint: stepCheckpoints) {
                        if(isReadyToRecover(checkpoint)) {
                            try {
                                boolean success = recover(checkpoint);
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
            }
        } else {
            logger.error("Recovery process cannot proceed: supported timeoutTypes are not set");
        }

        return counter;
    }

    protected abstract boolean recover(Checkpoint checkpoint);

    protected boolean isReadyToRecover(Checkpoint checkpoint) {
        boolean result = false;
        if(checkpoint != null) {
            if(checkpoint.getEntityType() != null) {
                ExpirationPolicy expPolicy = getExpirationPolicy(checkpoint.getEntityType(), checkpoint.getTimeoutType());
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

    public void setTimeIterationStep(int timeIterationStep) {
        this.timeIterationStep = timeIterationStep;
    }
    public void setTimeIterationStepUnit(TimeUnit timeIterationStepUnit) {
        this.timeIterationStepUnit = timeIterationStepUnit;
    }

}
