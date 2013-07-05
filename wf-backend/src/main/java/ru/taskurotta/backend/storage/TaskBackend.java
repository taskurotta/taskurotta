package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.checkpoint.CheckpointServiceProvider;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:11 PM
 */
public interface TaskBackend extends CheckpointServiceProvider {

    public void startProcess(TaskContainer taskContainer);


    /**
     * All resolved promise arguments should be swapped to original value objects.
     * Task state should be changed to "process"
     *
     * @param taskId
     * @return
     */
    public TaskContainer getTaskToExecute(UUID taskId, UUID processId);


    /**
     * Return task as it was registered
     *
     * @param taskId
     * @return
     */
    public TaskContainer getTask(UUID taskId, UUID processId);


    /**
     * Create RELEASE_TIMEOUT checkpoint
     *
     * @param taskDecision
     */
    public void addDecision(DecisionContainer taskDecision);

    /**
     * Idempotent getter for task decisions
     */
    public DecisionContainer getDecision(UUID taskId, UUID processId);

    /**
     * Delete RELEASE_TIMEOUT checkpoint
     *
     * @param taskDecision
     */
    public void addDecisionCommit(DecisionContainer taskDecision);



    public List<TaskContainer> getAllRunProcesses();


    /**
     * Return all decisions for particular process in the right chronological order.
     *
     * @param processId
     * @return
     */
    public List<DecisionContainer> getAllTaskDecisions(UUID processId);

}
