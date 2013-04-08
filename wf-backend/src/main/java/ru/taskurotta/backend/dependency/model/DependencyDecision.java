package ru.taskurotta.backend.dependency.model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 6:46 PM
 */
public class DependencyDecision {

    private List<UUID> readyTasks;
    private boolean isProcessFinished;
    private UUID finishedProcessId;
    private String finishedProcessValue;
    private boolean fail = false;

    public void addReadyTask(UUID taskId) {

        if (readyTasks == null) {
            readyTasks = new LinkedList<UUID>();
        }

        readyTasks.add(taskId);
    }

    public void addReadyTasks(UUID[] taskIds) {

        if (readyTasks == null) {
            readyTasks = new LinkedList<UUID>();
        }

        readyTasks.addAll(Arrays.asList(taskIds));
    }

    public DependencyDecision withFail() {
        fail = true;
        return this;
    }

    public DependencyDecision withReadyTasks(UUID[] taskIds) {
        addReadyTasks(taskIds);
        return this;
    }

    public List<UUID> getReadyTasks() {
        return readyTasks;
    }

    public boolean isProcessFinished() {
        return isProcessFinished;
    }

    public void setProcessFinished(boolean processFinished) {
        isProcessFinished = processFinished;
    }

    public UUID getFinishedProcessId() {
        return finishedProcessId;
    }

    public String getFinishedProcessValue() {
        return finishedProcessValue;
    }

    public void setFinishedProcessId(UUID finishedProcessId) {
        this.finishedProcessId = finishedProcessId;
    }

    public void setFinishedProcessValue(String finishedProcessValue) {
        this.finishedProcessValue = finishedProcessValue;
    }

    @Override
    public String toString() {
        return "DependencyDecision{" +
                "readyTasks=" + readyTasks +
                ", isProcessFinished=" + isProcessFinished +
                ", finishedProcessId=" + finishedProcessId +
                ", finishedProcessValue='" + finishedProcessValue + '\'' +
                ", fail=" + fail +
                '}';
    }
}

