package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.transport.model.TaskContainer;

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
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_SCHEDULE_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_COMMIT, task.getProcessId(), task.getActorId(), task.getStartTime()));

        //method body

    }

    @Override
    public void startProcessCommit(TaskContainer task) {

        //should be at the end of the method
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
        checkpointService.removeEntityCheckpoints(task.getProcessId(), TimeoutType.PROCESS_START_TO_COMMIT);
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {

        //should be at the end of the method
        checkpointService.removeEntityCheckpoints(processId, TimeoutType.PROCESS_START_TO_CLOSE);
        checkpointService.removeEntityCheckpoints(processId, TimeoutType.PROCESS_SCHEDULE_TO_CLOSE);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

}
