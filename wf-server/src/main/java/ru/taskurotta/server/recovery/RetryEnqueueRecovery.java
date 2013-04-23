package ru.taskurotta.server.recovery;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.recovery.base.AbstractIterableRecovery;

import java.util.UUID;

/**
 * Recovery process that tries to enqueue expired task again.
 * Requires CheckpointService as Checkpoint source.
 * Tries to recover all types of checkpoints if other is not explicitly set
 *
 */
public class RetryEnqueueRecovery extends AbstractIterableRecovery {

    private TimeoutType[] supportedTimeouts = TimeoutType.values();//tries to recover all types of timeouts by default
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;

    @Override
    protected boolean recover(Checkpoint checkpoint) {
        boolean result = false;
        TimeoutType timeoutType = checkpoint.getTimeoutType();

        //TODO: make it in some better way
        if(timeoutType.toString().toUpperCase().startsWith("TASK")) {//try to enqueue task again
            result = retryTaskEnqueue(checkpoint.getEntityGuid());
        } else if(timeoutType.toString().toUpperCase().startsWith("PROCESS")) {//Try to recover process by enqueue first task
            retryProcessStartTaskEnqueue(checkpoint.getEntityGuid());
        } else {
            logger.error("Unknown timeout type [{}] - cannot recover!", timeoutType);
        }
        return result;
    }


    private boolean retryTaskEnqueue(UUID taskGuid) {
        boolean result = false;
        TaskContainer task = taskBackend.getTask(taskGuid);
        if(task!=null) {
            queueBackend.enqueueItem(task.getActorId(), task.getTaskId(), task.getStartTime(), null);
            result = true;
        } else {
            logger.error("Task not found by GUID["+taskGuid+"]");
        }
        return result;
    }

    private boolean retryProcessStartTaskEnqueue(UUID processGuid) {
        boolean result = false;

        //TODO: requires some logic here :)

        return result;
    }

    public void setTaskBackend(TaskBackend taskBackend) {
        this.taskBackend = taskBackend;
    }

    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }

    @Override
    protected TimeoutType[] getSupportedTimeouts() {
        //TODO: ask for this the Checkpoint service itself?
        return supportedTimeouts;
    }

    //Optional to override defaults
    public void setSupportedTimeouts(TimeoutType[] supportedTimeouts) {
        this.supportedTimeouts = supportedTimeouts;
    }

}
