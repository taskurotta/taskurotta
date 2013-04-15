package ru.taskurotta.server.recovery;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.config.expiration.ExpirationPolicy;
import ru.taskurotta.server.recovery.base.AbstractScheduledIterableRecovery;

public class TaskExpirationRecovery extends AbstractScheduledIterableRecovery {

    private static final Logger logger = LoggerFactory.getLogger(TaskExpirationRecovery.class);

    private QueueBackend queueBackend;
    private TaskBackend taskBackend;

    @Override
    protected int processStep(int stepNumber, long timeFrom, long timeTill) {
        int counter = 0;
        CheckpointService checkpointService = taskBackend.getCheckpointService();

        CheckpointQuery query = new CheckpointQuery(TimeoutType.TASK_START_TO_CLOSE);
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
                        logger.error("Cannot recover task[" + task.getTaskId() + "] by TimeoutType["+TimeoutType.TASK_START_TO_CLOSE+"]", e);
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

    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }
    public void setTaskBackend(TaskBackend taskBackend) {
        this.taskBackend = taskBackend;
    }
    public void setConfigBackend(ConfigBackend configBackend) {
        initConfigs(configBackend.getActorPreferences());//initialize expiration policies
    }

}
