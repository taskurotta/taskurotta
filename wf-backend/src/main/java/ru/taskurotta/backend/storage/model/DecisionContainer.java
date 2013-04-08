package ru.taskurotta.backend.storage.model;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

import java.util.Arrays;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:14 PM
 */
public class DecisionContainer {

    private UUID taskId;
    private UUID processId;
    private ArgContainer value;
    private boolean isError = false;
    private ErrorContainer errorContainer;
    private TaskContainer[] tasks;

    public DecisionContainer(UUID taskId, UUID processId, ArgContainer value, boolean error,
                             ErrorContainer errorContainer,
                             TaskContainer[] tasks) {
        this.taskId = taskId;
        this.processId = processId;
        this.value = value;
        isError = error;
        this.errorContainer = errorContainer;
        this.tasks = tasks;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public ArgContainer getValue() {
        return value;
    }

    public boolean isError() {
        return isError;
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

    @Override
    public String toString() {
        return "DecisionContainer{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", value=" + value +
                ", isError=" + isError +
                ", errorContainer=" + errorContainer +
                ", tasks=" + (tasks == null ? null : Arrays.asList(tasks)) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecisionContainer)) return false;

        DecisionContainer that = (DecisionContainer) o;

        if (isError != that.isError) return false;
        if (errorContainer != null ? !errorContainer.equals(that.errorContainer) : that.errorContainer != null)
            return false;
        if (!processId.equals(that.processId)) return false;
        if (!taskId.equals(that.taskId)) return false;
        if (!Arrays.equals(tasks, that.tasks)) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId.hashCode();
        result = 31 * result + processId.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (isError ? 1 : 0);
        result = 31 * result + (errorContainer != null ? errorContainer.hashCode() : 0);
        result = 31 * result + (tasks != null ? Arrays.hashCode(tasks) : 0);
        return result;
    }
}
