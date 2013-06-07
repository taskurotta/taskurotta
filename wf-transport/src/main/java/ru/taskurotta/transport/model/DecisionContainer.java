package ru.taskurotta.transport.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:14 PM
 */
public class DecisionContainer implements Serializable {

    private UUID taskId;
    private UUID processId;
    private ArgContainer value;
    private ErrorContainer errorContainer;
    private long restartTime;
    private TaskContainer[] tasks;

    public DecisionContainer() {
    }

    public DecisionContainer(UUID taskId, UUID processId, ArgContainer value,
                             ErrorContainer errorContainer, long restartTime,
                             TaskContainer[] tasks) {
        this.taskId = taskId;
        this.processId = processId;
        this.value = value;
        this.errorContainer = errorContainer;
        this.restartTime = restartTime;
        this.tasks = tasks;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public ArgContainer getValue() {
        return value;
    }

    public boolean containsError() {
        return errorContainer != null;
    }

    public ErrorContainer getErrorContainer() {
        return errorContainer;
    }

    public TaskContainer[] getTasks() {
        return tasks;
    }

    public UUID getProcessId() {
        return processId;
    }

    public long getRestartTime() {
        return restartTime;
    }

    @Override
    public String toString() {
        return "DecisionContainer{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", value=" + value +
                ", errorContainer=" + errorContainer +
                ", restartTime=" + restartTime +
                ", tasks=" + Arrays.toString(tasks) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecisionContainer that = (DecisionContainer) o;

        if (restartTime != that.restartTime) return false;
        if (errorContainer != null ? !errorContainer.equals(that.errorContainer) : that.errorContainer != null)
            return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (!Arrays.equals(tasks, that.tasks)) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (errorContainer != null ? errorContainer.hashCode() : 0);
        result = 31 * result + (int) (restartTime ^ (restartTime >>> 32));
        result = 31 * result + (tasks != null ? Arrays.hashCode(tasks) : 0);
        return result;
    }
}
