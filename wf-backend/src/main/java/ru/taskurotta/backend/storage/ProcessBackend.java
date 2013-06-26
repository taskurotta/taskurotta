package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.checkpoint.CheckpointServiceProvider;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 7:58 PM
 */
public interface ProcessBackend extends CheckpointServiceProvider {

    /**
     * Create PROCESS_START_TIMEOUT checkpoint.
     *
     * @param task
     */
    public void startProcess(TaskContainer task);

    /**
     * Create PROCESS_TIMEOUT checkpoint.
     * Delete PROCESS_START_TIMEOUT checkpoint.
     *
     * @param task
     */
    public void startProcessCommit(TaskContainer task);

    /**
     * Delete PROCESS_TIMEOUT checkpoint.
     *
     * @param processId
     * @param returnValue JSON
     */
    public void finishProcess(DependencyDecision dependencyDecision, String returnValue);
}
