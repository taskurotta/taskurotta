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
    private ErrorContainer errorContainer;
    private TaskContainer[] tasks;

    public DecisionContainer(UUID taskId, UUID processId, ArgContainer value,
                             ErrorContainer errorContainer,
                             TaskContainer[] tasks) {
        this.taskId = taskId;
        this.processId = processId;
        this.value = value;
        this.errorContainer = errorContainer;
        this.tasks = tasks;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public ArgContainer getValue() {
        return value;
    }

    public boolean containsError() {
        return errorContainer!=null;
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
        return "DecisionContainer [taskId=" + taskId + ", processId="
                + processId + ", value=" + value + ", errorContainer="
                + errorContainer + ", tasks=" + Arrays.toString(tasks) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DecisionContainer other = (DecisionContainer) obj;
        if (errorContainer == null) {
            if (other.errorContainer != null)
                return false;
        } else if (!errorContainer.equals(other.errorContainer))
            return false;
        if (processId == null) {
            if (other.processId != null)
                return false;
        } else if (!processId.equals(other.processId))
            return false;
        if (taskId == null) {
            if (other.taskId != null)
                return false;
        } else if (!taskId.equals(other.taskId))
            return false;
        if (!Arrays.equals(tasks, other.tasks))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((errorContainer == null) ? 0 : errorContainer.hashCode());
        result = prime * result
                + ((processId == null) ? 0 : processId.hashCode());
        result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
        result = prime * result + Arrays.hashCode(tasks);
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }
}
