package ru.taskurotta.server.recovery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.recovery.base.AbstractIterableRecovery;

public class ProcessBackendEnqueueTaskRecovery extends AbstractIterableRecovery {

    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private ProcessBackend processBackend;

    @Override
    protected CheckpointService getCheckpointService() {
        return processBackend.getCheckpointService();
    }

    private List<TaskContainer> getProcessTasks(UUID processId) {
        List<TaskContainer> result = null;
        List<TaskContainer> tasks = taskBackend.getAllRunProcesses();
        if(tasks!=null && !tasks.isEmpty()) {
            result = new ArrayList<TaskContainer>();
            for(TaskContainer item: tasks) {
                if(processId.equals(item.getProcessId())) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    @Override
    protected boolean recover(Checkpoint checkpoint, TimeoutType timeoutType) {
        boolean result = false;

        //TODO: Checkpoint.guid is a process guid. Should it be process starter task guid instead? And recovery would just enqueue that task again?
        List<TaskContainer> processTasks = getProcessTasks(checkpoint.getGuid());
        if(processTasks!=null && processTasks.isEmpty()) {
            for(TaskContainer task: processTasks) {
                queueBackend.enqueueItem(task.getActorId(), task.getTaskId(), task.getStartTime(), null);
            }
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

    public void setProcessBackend(ProcessBackend processBackend) {
        this.processBackend = processBackend;
    }

}
