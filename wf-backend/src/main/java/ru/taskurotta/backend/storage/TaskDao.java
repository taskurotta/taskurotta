package ru.taskurotta.backend.storage;

import java.util.UUID;

import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * User: moroz
 * Date: 09.04.13
 */
public interface TaskDao {
    TaskContainer getTask(UUID taskId);

    void addDecision(DecisionContainer taskDecision);

    void addTask(TaskContainer taskContainer);

    DecisionContainer getDecision(UUID taskId);

    void markTaskProcessing(UUID taskId, boolean inProcess);

    boolean isTaskInProgress(UUID taskId);

    boolean isTaskReleased(UUID taskId);
}
