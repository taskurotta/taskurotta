package ru.taskurotta.server.recovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.config.expiration.ExpirationPolicy;

public class TaskExpirationRecovery implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TaskExpirationRecovery.class);

    private QueueBackend queueBackend;
    private TaskBackend taskBackend;

    private String schedule;

    private int timeIterationStep = 10000;
    private TimeUnit timeIterationStepUnit = TimeUnit.MILLISECONDS;

    private int recoveryPeriod = 60;
    private TimeUnit recoveryPeriodUnit = TimeUnit.MINUTES;

    private Map<String, ExpirationPolicy> expirationPolicyMap;

    @Override
    public void run() {
        logger.debug("TaskExpirationRecovery daemon started. Schedule[{}], expirationPolicies for[{}]", schedule, expirationPolicyMap!=null? expirationPolicyMap.keySet(): null);
        while(repeat(schedule)) {
            processRecoveryIteration();
        }
    }

    protected void processRecoveryIteration() {
        int counter = 0;
        long timeFrom = System.currentTimeMillis() - recoveryPeriodUnit.toMillis(recoveryPeriod);
        while(timeFrom < System.currentTimeMillis()) {
            long timeTill =  timeFrom+timeIterationStepUnit.toMillis(timeIterationStep);
            counter += processStep(TimeoutType.TASK_START_TO_CLOSE, timeFrom, timeTill);
            timeFrom = timeTill;
        }

        logger.info("Recovered [{}] tasks for timeout type[{}]", counter, TimeoutType.TASK_START_TO_CLOSE);

    }

    private int processStep(TimeoutType timeoutType, long timeFrom, long timeTill) {
        int counter = 0;
        CheckpointService checkpointService = taskBackend.getCheckpointService();

        CheckpointQuery query = new CheckpointQuery(timeoutType);
        query.setMaxTime(timeTill);
        query.setMinTime(timeFrom);

        List<Checkpoint> stepCheckpoints = checkpointService.listCheckpoints(query);

        if(stepCheckpoints!= null && !stepCheckpoints.isEmpty()) {
            for(Checkpoint checkpoint: stepCheckpoints) {
                if(isReadyToRecover(checkpoint)) {
                    TaskContainer task = taskBackend.getTask(checkpoint.getGuid());
                    try {
                        queueBackend.enqueueItem(task.getActorId(), task.getTaskId(), task.getStartTime(), null);
                        checkpointService.removeCheckpoint(checkpoint);
                        counter++;
                    } catch (Exception e) {
                        logger.error("Cannot recover task[" + task.getTaskId() + "]", e);
                    }
                }
            }
        }

        return counter;
    }


    private boolean isReadyToRecover(Checkpoint checkpoint) {
        boolean result = false;
        if(checkpoint != null) {
            if(checkpoint.getEntityType() != null) {
                ExpirationPolicy expPolicy = getExpirationPolicy(checkpoint.getEntityType());
                if(expPolicy != null) {
                    long timeout = expPolicy.getExpirationTimeout(System.currentTimeMillis());
                    result = expPolicy.readyToRecover(checkpoint.getGuid())
                            && (System.currentTimeMillis() > (checkpoint.getTime()+timeout));
                }
            }

        }
        return result;
    }

    private ExpirationPolicy getExpirationPolicy(String entityType) {
        ExpirationPolicy result = null;
        if(entityType!=null && expirationPolicyMap!=null) {
            result = expirationPolicyMap.get(entityType);
            if(result == null) {
                result = expirationPolicyMap.get("default");
            }
        }
        return result;
    }

    private void initConfigs(ActorPreferences[] actorPrefs) {
        logger.debug("Initializing recovery config with actorPrefs[{}]", actorPrefs);
        if (actorPrefs != null) {
            try {
                expirationPolicyMap = new HashMap<String, ExpirationPolicy>();
                for(ActorPreferences actorConfig: actorPrefs) {
                    ExpirationPolicy expPolicy = null;
                    ActorPreferences.ExpirationPolicyConfig expPolicyConf = actorConfig.getExpirationPolicy();

                    if (expPolicyConf != null) {
                        Class<?> expPolicyClass = Class.forName(expPolicyConf.getClassName());
                        Properties expPolicyProps = expPolicyConf.getProperties();

                        if (expPolicyProps != null) {
                            expPolicy = (ExpirationPolicy) expPolicyClass.getConstructor(Properties.class).newInstance(expPolicyProps);
                        } else {
                            expPolicy = (ExpirationPolicy) expPolicyClass.newInstance();
                        }

                        expirationPolicyMap.put(actorConfig.getId(), expPolicy);
                    }

                }

            } catch (Exception e) {
                logger.error("TaskExpirationRecovery#initConfigs invocation failed! actorPrefs[" + actorPrefs + "]", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    private static boolean repeat(String schedule) {
        if (schedule == null) {
            return false;
        }
        Integer number = Integer.valueOf(schedule.replaceAll("\\D", "").trim());
        TimeUnit unit = TimeUnit.valueOf(schedule.replaceAll("\\d", "").trim());
        try {
            Thread.sleep(unit.toMillis(number));
        } catch (InterruptedException e) {
            logger.error("TaskExpirationRecovery schedule interrupted", e);
        }
        return true;
    }

    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }
    public void setTaskBackend(TaskBackend taskBackend) {
        this.taskBackend = taskBackend;
    }
    public void setConfigBackend(ConfigBackend configBackend) {
        initConfigs(configBackend.getActorPreferences());
    }
    public void setTimeIterationStep(int timeIterationStep) {
        this.timeIterationStep = timeIterationStep;
    }
    public void setRecoveryPeriod(int recoveryPeriod) {
        this.recoveryPeriod = recoveryPeriod;
    }
    public void setRecoveryPeriodUnit(TimeUnit recoveryPeriodUnit) {
        this.recoveryPeriodUnit = recoveryPeriodUnit;
    }
    public void setTimeIterationStepUnit(TimeUnit timeIterationStepUnit) {
        this.timeIterationStepUnit = timeIterationStepUnit;
    }

}
