package ru.taskurotta.server.transport;

import ru.taskurotta.backend.storage.model.AsyncProcess;
import ru.taskurotta.backend.storage.model.AsyncTask;
import ru.taskurotta.backend.storage.model.AsyncTaskError;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:23 PM
 */
public class BackendConverter {

    public static AsyncProcess toAsyncProcess(TaskContainer taskContainer) {

        // TODO: implement it
        return null;
    }


    public static TaskContainer toTaskContainer(AsyncTask asyncTask) {

        // TODO: implement it
        return null;
    }


    public static AsyncTaskError toAsyncTaskError(UUID taskId, ErrorContainer errorContainer) {

        // TODO: implement it
        return  null;
    }
}
