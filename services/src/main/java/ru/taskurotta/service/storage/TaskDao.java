package ru.taskurotta.service.storage;

import ru.taskurotta.internal.TaskUID;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.transport.model.Decision;
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

    void updateTimeout(UUID taskId, UUID processId, long workerTimeout);

    Decision startTask(UUID taskId, UUID processId, long workerTimeout, boolean failOnWorkerTimeout);

    /**
     * @param taskId
     * @param processId
     * @param force        restart task anyway
     * @param ifFatalError restart only if decision container has fatal error
     * @return
     */
    boolean restartTask(UUID taskId, UUID processId, boolean force, boolean ifFatalError);

    boolean retryTask(UUID taskId, UUID processId, long workerTimeout);

    Decision finishTask(DecisionContainer taskDecision);

    TaskContainer getTask(UUID taskId, UUID processId);

    void addTask(TaskContainer taskContainer);

    void updateTask(TaskContainer taskContainer);

    Decision getDecision(UUID taskId, UUID processId);

    DecisionContainer getDecisionContainer(UUID taskId, UUID processId);

    boolean isTaskReleased(UUID taskId, UUID processId);

    GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    List<TaskContainer> getRepeatedTasks(int iterationCount);

    void deleteTasks(Set<UUID> taskIds, UUID processId);

    void deleteDecisions(Set<UUID> decisionsIds, UUID processId);

    void archiveProcessData(UUID processId, Collection<UUID> finishedTaskIds);

    List<TaskContainer> findTasks(TaskSearchCommand command);

    void updateTaskDecision(DecisionContainer taskDecision);

    ResultSetCursor<TaskUID> findIncompleteTasks(long lastRecoveryTime, int batchSize);

}