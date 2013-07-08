package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.List;
import java.util.UUID;

/**
 * User: moroz
 * Date: 09.04.13
 */
public interface TaskDao {
    public TaskContainer getTask(UUID taskId);

    public void addDecision(DecisionContainer taskDecision);

    public void addTask(TaskContainer taskContainer);

    public void updateTask(TaskContainer taskContainer);

    public DecisionContainer getDecision(UUID taskId);

    public boolean isTaskReleased(UUID taskId);

    public List<TaskContainer> getProcessTasks(UUID processUuid);

    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    public List<TaskContainer> getRepeatedTasks(int iterationCount);

    public TaskContainer removeTask(UUID taskId);

}
