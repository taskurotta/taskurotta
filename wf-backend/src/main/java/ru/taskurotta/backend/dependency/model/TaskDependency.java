package ru.taskurotta.backend.dependency.model;

import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:46 PM
 */
public class TaskDependency {

    private UUID taskId;

    private List<UUID> thatWaitThis;
    private List<UUID> thisWaitThat;
    private boolean parentWaitIt = false;
    private UUID parentId;
    private int countdown;

    public TaskDependency() {
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public List<UUID> getThatWaitThis() {
        return thatWaitThis;
    }

    public void setThatWaitThis(List<UUID> thatWaitThis) {
        this.thatWaitThis = thatWaitThis;
    }

    public boolean isParentWaitIt() {
        return parentWaitIt;
    }

    public void setParentWaitIt(boolean parentWaitIt) {
        this.parentWaitIt = parentWaitIt;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public List<UUID> getThisWaitThat() {
        return thisWaitThat;
    }

    public void setThisWaitThat(List<UUID> thisWaitThat) {
        this.thisWaitThat = thisWaitThat;
    }
}
