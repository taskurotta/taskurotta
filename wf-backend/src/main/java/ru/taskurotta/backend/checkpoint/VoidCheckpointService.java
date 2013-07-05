package ru.taskurotta.backend.checkpoint;

import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checkpoint service that simply doesn't do anything.
 * Can be used as a mock stub for preventing NPE on checkpoint service calls
 * User: dimadin
 * Date: 26.04.13
 * Time: 10:15
 */
public class VoidCheckpointService implements CheckpointService {

    @Override
    public void addCheckpoint(Checkpoint checkpoint) {
        return;
    }

    @Override
    public void addCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        return;
    }

    @Override
    public void removeCheckpoint(Checkpoint checkpoint) {
        return;
    }

    @Override
    public void removeCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoint) {
        return;
    }

    @Override
    public List<Checkpoint> listCheckpoints(CheckpointQuery command) {
        return new ArrayList<Checkpoint>();
    }

    @Override
    public int removeTaskCheckpoints(UUID uuid, UUID processId, TimeoutType timeoutType) {
        return 0;
    }
}
