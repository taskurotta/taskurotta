package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.storage.model.AsyncProcess;
import ru.taskurotta.backend.storage.model.AsyncTask;
import ru.taskurotta.backend.storage.model.AsyncTaskError;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:11 PM
 */
public class StorageBackend {

    public void createNewProcess(AsyncProcess asyncProcess) {

    }


    /**
     * All resolved promise arguments should be swapped to original value objects.
     *
     * @param taskId
     * @return
     */
    public AsyncTask getTaskToExecute(UUID taskId) {
        return null;
    }

    /**
     * Return task as it was registered
     *
     * @param taskId
     * @return
     */
    public AsyncTask getTask(UUID taskId) {
        return null;
    }


    /**
     * @param asyncTaskError
     * @param isShouldBeRestarted retry counter should be incremented
     */
    public void logError(AsyncTaskError asyncTaskError, boolean isShouldBeRestarted) {

    }

}
