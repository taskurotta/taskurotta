package ru.taskurotta.backend.checkpoint;

import java.util.List;

import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

public interface CheckpointService {

    public void addCheckpoint(Checkpoint checkpoint);

    public void addCheckpoints(String type, List<Checkpoint> checkpoints);

    public void removeCheckpoint(Checkpoint checkpoint);

    public void removeCheckpoints(String type, List<Checkpoint> checkpoint);

    public List<Checkpoint> listCheckpoints(CheckpointQuery command);

}
