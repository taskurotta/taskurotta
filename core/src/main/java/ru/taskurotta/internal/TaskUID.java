package ru.taskurotta.internal;

import java.util.UUID;

/**
 */
public class TaskUID {

    UUID taskId;
    UUID processId;

    public TaskUID(UUID taskId, UUID processId) {
        this.taskId = taskId;
        this.processId = processId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskUID taskKey1 = (TaskUID) o;

        if (!processId.equals(taskKey1.processId)) return false;
        if (!taskId.equals(taskKey1.taskId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId.hashCode();
        result = 31 * result + processId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TaskKey{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                '}';
    }
}
