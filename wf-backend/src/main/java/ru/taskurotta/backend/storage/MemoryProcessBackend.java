package ru.taskurotta.backend.storage;

import java.util.UUID;

import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 8:02 PM
 */
public class MemoryProcessBackend implements ProcessBackend {

    @Override
    public void startProcess(TaskContainer task) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void startProcessCommit(UUID processId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
