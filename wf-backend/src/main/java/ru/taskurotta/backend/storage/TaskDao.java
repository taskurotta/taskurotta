package ru.taskurotta.backend.storage;

import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

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
}
