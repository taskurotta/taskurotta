package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User: moroz
 * Date: 09.04.13
 */
public interface TaskDao {
    TaskContainer getTask(UUID taskId, UUID processId);

    void addDecision(DecisionContainer taskDecision);

    void addTask(TaskContainer taskContainer);

    void updateTask(TaskContainer taskContainer);

    DecisionContainer getDecision(UUID taskId, UUID processId);

    boolean isTaskReleased(UUID taskId, UUID processId);

    GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    List<TaskContainer> getRepeatedTasks(int iterationCount);

    TaskContainer removeTask(UUID taskId, UUID processId);

    void archiveProcessData(UUID processId, Collection<UUID> finishedTaskIds);

    List<TaskContainer> findTasks(TaskSearchCommand command);
}
