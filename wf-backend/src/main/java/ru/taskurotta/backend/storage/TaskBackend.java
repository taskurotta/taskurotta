package ru.taskurotta.backend.storage;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:11 PM
 */
public interface TaskBackend {

    public void startProcess(TaskContainer taskContainer);


    /**
     * All resolved promise arguments should be swapped to original value objects.
     * Task state should be changed to "process"
     *
     * @param taskId
     * @return
     */
    public TaskContainer getTaskToExecute(UUID taskId);


    /**
     * Return task as it was registered
     *
     * @param taskId
     * @return
     */
    public TaskContainer getTask(UUID taskId);


    /**
     * Create RELEASE_ERROR_TIMEOUT checkpoint
     *
     * @param asyncTaskError
     * @param shouldBeRestarted retry counter should be incremented
     */
    public void addError(UUID taskId, ErrorContainer asyncTaskError, boolean shouldBeRestarted);


    /**
     * Create RELEASE_TIMEOUT checkpoint
     *
     * @param taskDecision
     */
    public void addDecision(DecisionContainer taskDecision);


    /**
     * Delete RELEASE_TIMEOUT checkpoint
     *
     * @param taskId
     */
    public void addDecisionCommit(UUID taskId);


    /**
     * Delete RELEASE_ERROR_TIMEOUT
     *
     * @param taskId
     */
    public void addErrorCommit(UUID taskId);


    public List<TaskContainer> getAllRunProcesses();


    /**
     * Return all decisions for particular process in the right chronological order.
     *
     * @param processId
     * @return
     */
    public List<DecisionContainer> getAllTaskDecisions(UUID processId);
}
