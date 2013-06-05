package ru.taskurotta.backend.dependency.graph;

import java.util.List;
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

    public boolean scheduleNode(UUID id, UUID processId);

    public List<TaskNode> getActiveProcessNodes(UUID processId);

    public int deleteProcessNodes(UUID processId);

}
