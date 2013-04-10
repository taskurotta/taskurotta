package ru.taskurotta.backend.ora.storage;

import java.util.UUID;

import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * User: moroz
 * Date: 10.04.13
 */
public class OraTaskDao implements TaskDao {
    @Override
    public void addDecision(DecisionContainer taskDecision) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TaskContainer getTask(UUID taskId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DecisionContainer getDecision(UUID taskId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void markTaskProcessing(UUID taskId, boolean inProcess) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isTaskInProgress(UUID taskId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isTaskReleased(UUID taskId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
