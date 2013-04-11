package ru.taskurotta.server.recovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskDefinition;
import ru.taskurotta.server.config.expiration.ExpirationPolicy;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

public class TaskExpirationRecovery implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TaskExpirationRecovery.class);

    private QueueBackend queueBackend;
    private TaskBackend taskBackend;

    private String schedule;
    private long timeIterationStep = 10000;
    private int recoveryPeriod = 60;
    private TimeUnit recoveryPeriodUnit = TimeUnit.MINUTES;

    private Map<ActorDefinition, ExpirationPolicy> expirationPolicyMap;

    @Override
    public void run() {
        logger.debug("TaskExpirationRecovery daemon started. Schedule[{}], expirationPolicies for[{}]", schedule, expirationPolicyMap!=null? expirationPolicyMap.keySet(): null);
        while(repeat(schedule)) {
            if(expirationPolicyMap!=null && !expirationPolicyMap.isEmpty()) {
                for(ActorDefinition actorDef: expirationPolicyMap.keySet()) {
                    ExpirationPolicy ePolicy =  expirationPolicyMap.get(actorDef);
                    int counter = 0;
                    long timeFrom = System.currentTimeMillis() - recoveryPeriodUnit.toMillis(recoveryPeriod);
                    while(timeFrom < System.currentTimeMillis()) {
                        long timeTill =  timeFrom+timeIterationStep;
                        counter += processStep(ActorUtils.getActorId(actorDef), timeFrom, timeTill, ePolicy);
                        timeFrom = timeTill;
                    }

                    logger.info("Recovered [{}] tasks due to expiration policy of actor[{}]", counter,  actorDef);
                }
            }
        }
    }

    private static List<Checkpoint> filterNonExpired(List<Checkpoint> activeTasks, long timeout) {
        if(activeTasks!=null && !activeTasks.isEmpty()) {
            int removed = 0;
            int initialSize = activeTasks.size();
            Iterator<Checkpoint> iterator = activeTasks.iterator();
            while(iterator.hasNext()) {
                Checkpoint item = iterator.next();
                logger.debug("Validating item[{}], timeout[{}], currentTime[{}]", item, timeout, System.currentTimeMillis());
                if(item.getTime()+timeout > System.currentTimeMillis()) {//not expired
                    iterator.remove();
                    removed++;
                }
            }
            logger.debug("Removed [{}] items from active task list sized[{}] with timeout[{}]", removed, initialSize, timeout);
        }
        return activeTasks;
    }

    //    private static List<TaskDefinition> filterNonExpired(List<TaskDefinition> activeTasks, long timeout) {
    //        if(activeTasks!=null && !activeTasks.isEmpty()) {
    //            int removed = 0;
    //            int initialSize = activeTasks.size();
    //            Iterator<TaskDefinition> iterator = activeTasks.iterator();
    //            while(iterator.hasNext()) {
    //                TaskDefinition item = iterator.next();
    //                logger.debug("Validating item[{}], ExecutionStarted[{}], timeout[{}], currentTime[{}]", item.getTaskId(), item.getExecutionStarted(), timeout, System.currentTimeMillis());
    //                if(item.getExecutionStarted()+timeout > System.currentTimeMillis()) {//not expired
    //                    iterator.remove();
    //                    removed++;
    //                }
    //            }
    //            logger.debug("Removed [{}] items from active task list sized[{}] with timeout[{}]", removed, initialSize, timeout);
    //        }
    //        return activeTasks;
    //    }

    private int processStep(String actorId, long timeFrom, long timeTill, ExpirationPolicy expPolicy) {
        long timeout = expPolicy.getExpirationTimeout(System.currentTimeMillis());
        CheckpointService checkpointService = taskBackend.getCheckpointService();

        CheckpointQuery query = new CheckpointQuery();
        query.setType(actorId);
        query.setMaxTime(timeTill);
        query.setMinTime(timeFrom);

        //TODO: Always return empty list for default actor! Implement default actor behavior
        List<Checkpoint> expired = filterNonExpired(checkpointService.listCheckpoints(query), timeout);
        int counter = 0;
        if(expired!=null && !expired.isEmpty()) {

            logger.debug("Try to recover [{}] expired tasks", expired.size());
            for(Checkpoint checkpoint: expired) {
                TaskContainer task = taskBackend.getTask(checkpoint.getGuid());
                if(expPolicy.readyToRecover(task.getTaskId())) {
                    try {
                        queueBackend.enqueueItem(task.getActorId(), task.getTaskId(), task.getStartTime());
                        checkpointService.removeCheckpoint(checkpoint);
                        counter++;
                    } catch(Exception e) {
                        logger.error("Cannot recover task["+task.getTaskId()+"]", e);
                    }
                } else {
                    logger.error("Cannot perform expired task recovery. Task[{}]", task);
                    //TODO: execute error processing in backends
                }
            }
        }

        return counter;
    }

    private void initConfigs(ActorPreferences[] actorPrefs) {
        logger.debug("Initializing recovery config with actorPrefs[{}]", actorPrefs);
        if(actorPrefs!=null) {
            try {
                expirationPolicyMap = new HashMap<ActorDefinition, ExpirationPolicy>();
                for(ActorPreferences actorConfig: actorPrefs) {
                    ExpirationPolicy expPolicy = null;
                    ActorPreferences.ExpirationPolicyConfig expPolicyConf = actorConfig.getExpirationPolicy();

                    if(expPolicyConf!=null) {
                        Class<?> expPolicyClass = Class.forName(expPolicyConf.getClassName());
                        Properties expPolicyProps = expPolicyConf.getProperties();

                        if(expPolicyProps != null) {
                            expPolicy = (ExpirationPolicy) expPolicyClass.getConstructor(Properties.class).newInstance(expPolicyProps);
                        } else {
                            expPolicy = (ExpirationPolicy) expPolicyClass.newInstance();
                        }

                        expirationPolicyMap.put(actorConfig.getActorDefinition(), expPolicy);
                    }

                }

            } catch(Exception e) {
                logger.error("TaskExpirationRecovery#initConfigs invocation failed! actorPrefs["+actorPrefs+"]", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    private static boolean repeat(String schedule) {
        if(schedule == null) {
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
    public void setTimeIterationStep(long timeIterationStep) {
        this.timeIterationStep = timeIterationStep;
    }
    public void setRecoveryPeriod(int recoveryPeriod) {
        this.recoveryPeriod = recoveryPeriod;
    }
    public void setRecoveryPeriodUnit(TimeUnit recoveryPeriodUnit) {
        this.recoveryPeriodUnit = recoveryPeriodUnit;
    }

}
