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
    private ArgContainer value;
    private boolean isError = false;
    private ErrorContainer errorContainer;
    private TaskContainer[] tasks;

    public DecisionContainer(UUID taskId, ArgContainer value, boolean error, ErrorContainer errorContainer, TaskContainer[] tasks) {
        this.taskId = taskId;
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

    @Override
    public String toString() {
        return "DecisionContainer{" +
                "taskId=" + taskId +
                ", value=" + value +
                ", isError=" + isError +
                ", errorContainer=" + errorContainer +
                ", tasks=" + (tasks == null ? null : Arrays.asList(tasks)) +
                '}';
    }
}
