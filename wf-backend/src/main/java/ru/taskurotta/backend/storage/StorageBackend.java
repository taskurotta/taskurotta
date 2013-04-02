package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:11 PM
 */
public interface StorageBackend {

    public void addProcess(TaskContainer taskContainer);


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
     * @param asyncTaskError
     * @param shouldBeRestarted retry counter should be incremented
     */
    public void addError(UUID taskId, ErrorContainer asyncTaskError, boolean shouldBeRestarted);


    public void addDecision(DecisionContainer taskDecision);


    /**
     * Task should be marked as successfully analysed.
     * Process should be marked as finished.
     *
     * @param taskId
     * @param processFinished
     */
    public void addDecisionCommit(UUID taskId, boolean processFinished);


    /**
     * Task should be marked as successfully analysed.
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
