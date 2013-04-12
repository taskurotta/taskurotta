package ru.taskurotta.backend.checkpoint;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

public interface CheckpointService {

    public void addCheckpoint(Checkpoint checkpoint);

    public void addCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints);

    public void removeCheckpoint(Checkpoint checkpoint);

    public void removeCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoint);

    public List<Checkpoint> listCheckpoints(CheckpointQuery command);

    //TODO: use listCheckpoints(CheckpointQuery query) with extended query object?
    public List<Checkpoint> getCheckpoints(UUID uuid, TimeoutType timeoutType);

}
