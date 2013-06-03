package ru.taskurotta.backend.dependency.graph;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 03.06.13 10:23
 */
public interface TaskNodeDao {

    public TaskNode getNode(UUID taskId, UUID processId);

    public void addNode(TaskNode taskNode);

    public boolean releaseNode(UUID id, UUID processId);

    public UUID[] getReadyTasks(UUID processId);

    public boolean isProcessReady(UUID processId);

}
