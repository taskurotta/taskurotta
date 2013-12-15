package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.Collection;
import java.util.List;
import java.util.Set;
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

    void deleteTasks(Set<UUID> taskIds, UUID processId);

    void deleteDecisions(Set<UUID> decisionsIds, UUID processId);

    void archiveProcessData(UUID processId, Collection<UUID> finishedTaskIds);

    List<TaskContainer> findTasks(TaskSearchCommand command);
}
