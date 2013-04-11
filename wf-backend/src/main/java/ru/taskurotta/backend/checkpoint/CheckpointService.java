package ru.taskurotta.backend.checkpoint;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

//TODO: check if it really works)
public interface CheckpointService {

    public void addCheckpoint(Checkpoint checkpoint);

    public void addCheckpoints(String type, List<Checkpoint> checkpoints);

    public void removeCheckpoint(Checkpoint checkpoint);

    public void removeCheckpoints(String type, List<Checkpoint> checkpoint);

    public List<Checkpoint> listCheckpoints(CheckpointQuery command);

    //TODO: use listCheckpoints(CheckpointQuery query) with extended query object?
    public List<Checkpoint> getCheckpoints(UUID uuid, String type);

}
