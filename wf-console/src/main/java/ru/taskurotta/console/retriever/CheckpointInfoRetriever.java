package ru.taskurotta.console.retriever;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;

import java.util.List;

/**
 * Checkpoint information retriever. Provides info on checkpoints, such as number of unchecked checkpoints, list of active checkpoints
 * User: dimadin
 * Date: 17.05.13 16:16
 */
public interface CheckpointInfoRetriever {

    public List<Checkpoint> getAllActiveCheckpoints();

    public List<Checkpoint> getActiveCheckpoints(TimeoutType type);

}
