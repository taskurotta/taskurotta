package ru.taskurotta.backend.dependency.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 6:46 PM
 */
public class DependencyDecision {

    private Set<UUID> readyTasks;
    private boolean isProcessFinished;
    private UUID finishedProcessId;
    private String finishedProcessValue;
    private boolean fail = false;

    public DependencyDecision() {
    }

    public DependencyDecision(UUID processId) {
        this.finishedProcessId = processId;
    }

    public void addReadyTask(UUID taskId) {

        if (readyTasks == null) {
            readyTasks = new HashSet<>();
        }

        readyTasks.add(taskId);
    }

    public void addReadyTasks(UUID[] taskIds) {

        if (readyTasks == null) {
            readyTasks = new HashSet<>();
        }

        readyTasks.addAll(Arrays.asList(taskIds));
    }

    public DependencyDecision withFail() {
        fail = true;
        return this;
    }

    public DependencyDecision withReadyTasks(UUID[] taskIds) {
		readyTasks = new HashSet<>(Arrays.asList(taskIds));
        return this;
    }

    public Set<UUID> getReadyTasks() {
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

    public boolean isFail() {
        return fail;
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

