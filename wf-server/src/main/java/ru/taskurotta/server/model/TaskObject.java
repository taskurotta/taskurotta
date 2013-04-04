package ru.taskurotta.server.model;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskStateObject;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 12:43 PM
 */
public class TaskObject extends TaskContainer {

    protected TaskStateObject state;
    protected List<TaskStateObject> stateHistory;

    protected List<UUID> waitingId;
    protected boolean isDependTask = false;
    protected UUID parentId;
    protected int countdown;

    protected ArgContainer value;
    protected boolean isError = false;
    protected ErrorContainer errorContainer;

    public TaskObject(TaskContainer taskContainer) {
        super(taskContainer.getTaskId(), taskContainer.getTarget(), taskContainer.getStartTime(),
                taskContainer.getNumberOfAttempts(), taskContainer.getArgs(), taskContainer.getOptions());
    }

    public int getCountdown() {
        return countdown;
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public ArgContainer getValue() {
        return value;
    }

    public TaskStateObject getState() {
        return state;
    }

    public List<TaskStateObject> getStateHistory() {
        return stateHistory;
    }

    public List<UUID> getWaitingId() {
        return waitingId;
    }

    public boolean isDependTask() {
        return isDependTask;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setState(TaskStateObject state) {
        this.state = state;
    }

    public void setStateHistory(List<TaskStateObject> stateHistory) {
        this.stateHistory = stateHistory;
    }

    public void setValue(ArgContainer value) {
        this.value = value;
    }

    public void setWaitingId(List<UUID> waitingId) {
        this.waitingId = waitingId;
    }

    public void setDependTask(boolean dependTask) {
        isDependTask = dependTask;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }


//    public void setState(TaskStateObject state) {
//
//        if (stateHistory == null) {
//            stateHistory = new ArrayList<TaskStateObject>();
//        }
//
//        stateHistory.add(state);
//        this.state = state;
//    }


    @Override
    public String toString() {
        return "TaskObject{" +
                "[" + super.toString() + "]" +
                "state=" + state +
                ", stateHistory=" + stateHistory +
                ", waitingId=" + waitingId +
                ", isDependTask=" + isDependTask +
                ", parentId=" + parentId +
                ", countdown=" + countdown +
                ", value=" + value +
                ", isError=" + isError +
                ", errorContainer=" + errorContainer +
                '}';
    }
}
