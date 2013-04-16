package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.storage.model.TaskContainer;

import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 8:02 PM
 */
public class MemoryProcessBackend implements ProcessBackend {

    private CheckpointService checkpointService = new MemoryCheckpointService();

    @Override
    public void startProcess(TaskContainer task) {
        //To change body of implemented methods use File | Settings | File Templates.

        Checkpoint startProcessCheckpoint = new Checkpoint(TimeoutType.PROCESS_START_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime());
        checkpointService.addCheckpoint(startProcessCheckpoint);
    }

    @Override
    public void startProcessCommit(TaskContainer task) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        checkpointService.removeEntityCheckpoints(processId, TimeoutType.PROCESS_START_TO_CLOSE);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

}
