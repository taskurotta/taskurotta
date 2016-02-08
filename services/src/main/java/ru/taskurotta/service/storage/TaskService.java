package ru.taskurotta.service.storage;

import ru.taskurotta.transport.model.Decision;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:11 PM
 */
public interface TaskService {

    void startProcess(TaskContainer taskContainer);


    /**
     * All resolved promise arguments should be swapped to original value objects.
     *
     * @param taskId - ID of the task
     * @return TaskContainer with the task
     */
    TaskContainer getTaskToExecute(UUID taskId, UUID processId, boolean simulate);


    /**
     * Return task as it was registered
     *
     * @param taskId - ID of the task
     * @return TaskContainer with the task
     */
    TaskContainer getTask(UUID taskId, UUID processId);


    Decision finishTask(DecisionContainer taskDecision);

    boolean retryTask(UUID taskId, UUID processId);

    /**
     * @param taskId
     * @param processId
     * @param force     restart task anyway
     * @param ifFatalError restart only if decision container has fatal error
     * @return
     */
    boolean restartTask(UUID taskId, UUID processId, boolean force, boolean ifFatalError);

    Decision getDecision(UUID taskId, UUID processId);

    DecisionContainer getDecisionContainer(UUID taskId, UUID processId);

    List<TaskContainer> getAllRunProcesses();


    /**
     * Return all decisions for particular process in the right chronological order.
     *
     * @param processId - ID of the process
     * @return List of DecisionContainer with all decisions for particular process in the right chronological order.
     */
    List<DecisionContainer> getAllTaskDecisions(UUID processId);

    /**
     * Clean up resources after process.
     * Service should avoid synchronous removing artifacts due to performance issues.
     *
     * @param processId       - ID of the process
     * @param finishedTaskIds - all task UUIDs of finished process
     */
    void finishProcess(UUID processId, Collection<UUID> finishedTaskIds);

    void updateTaskDecision(DecisionContainer taskDecision);

    void addNewTasks(DecisionContainer taskDecision);
}
