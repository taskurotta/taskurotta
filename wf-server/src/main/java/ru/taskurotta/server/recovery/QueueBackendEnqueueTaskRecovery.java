package ru.taskurotta.server.recovery;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.recovery.base.AbstractIterableRecovery;

public class QueueBackendEnqueueTaskRecovery extends AbstractIterableRecovery {

    private TaskBackend taskBackend;
    private QueueBackend queueBackend;

    @Override
    protected CheckpointService getCheckpointService() {
        return queueBackend.getCheckpointService();
    }

    private TaskContainer getTaskByCheckpoint(Checkpoint checkpoint) {
        return taskBackend.getTask(checkpoint.getGuid());
    }

    @Override
    protected boolean recover(Checkpoint checkpoint, TimeoutType timeoutType) {
        boolean result = false;
        TaskContainer task = getTaskByCheckpoint(checkpoint);
        if(task!=null) {
            queueBackend.enqueueItem(task.getActorId(), task.getTaskId(), task.getStartTime(), null);
            result = true;
        }
        return result;
    }

    public void setTaskBackend(TaskBackend taskBackend) {
        this.taskBackend = taskBackend;
    }

    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }


}
