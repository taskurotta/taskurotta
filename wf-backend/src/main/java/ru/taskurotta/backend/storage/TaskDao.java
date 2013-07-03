package ru.taskurotta.backend.storage;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * User: moroz
 * Date: 09.04.13
 */
public interface TaskDao {
    TaskContainer getTask(UUID taskId);

    void addDecision(DecisionContainer taskDecision);

    void addTask(TaskContainer taskContainer);

    void updateTask(TaskContainer taskContainer);

    DecisionContainer getDecision(UUID taskId);

    boolean isTaskReleased(UUID taskId);

    List<TaskContainer> getProcessTasks(UUID processUuid);

    GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    public List<TaskContainer> getRepeatedTasks(int iterationCount);
}
