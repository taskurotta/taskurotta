package ru.taskurotta.backend.storage;

import java.util.UUID;

import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 7:58 PM
 */
public interface ProcessBackend {

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
     * @param processId
     */
    public void startProcessCommit(UUID processId);

    /**
     * Delete PROCESS_TIMEOUT checkpoint.
     *
     * @param processId
     * @param returnValue JSON
     */
    public void finishProcess(UUID processId, String returnValue);
}
