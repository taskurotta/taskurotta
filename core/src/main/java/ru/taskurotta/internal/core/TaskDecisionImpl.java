package ru.taskurotta.internal.core;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.Arrays;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 28.12.12
 * Time: 16:40
 */
public class TaskDecisionImpl implements TaskDecision {


    private UUID uuid;
    private UUID processId;
    private Object value;
    private Task[] tasks;
    private Throwable exception;
    private boolean error;
    private long restartTime = NO_RESTART;
    private long executionTime;

    public TaskDecisionImpl(UUID uuid, UUID processId, Object value, Task[] tasks, long executionTime) {
        this.uuid = uuid;
        this.processId = processId;
        this.value = value;
        this.tasks = tasks;
        this.error = false;
        this.executionTime = executionTime;
    }

    public TaskDecisionImpl(UUID uuid, UUID processId, Throwable exception, Task[] tasks) {
        this.uuid = uuid;
        this.exception = exception;
        this.processId = processId;
        this.tasks = tasks;
        this.error = exception != null;
    }

    public TaskDecisionImpl(UUID uuid, UUID processId, Throwable exception, Task[] tasks, long restartTime) {
        this.uuid = uuid;
        this.exception = exception;
        this.processId = processId;
        this.tasks = tasks;
        this.error = exception != null;
        this.restartTime = restartTime;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isError() {
        return error;
    }

    @Override
    public UUID getId() {
        return uuid;
    }

    @Override
    public UUID getProcessId() {
        return processId;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Task[] getTasks() {
        return tasks;
    }

    @Override
    public long getRestartTime() {
        return restartTime;
    }

    @Override
    public long getExecutionTime() {
        return executionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskDecisionImpl that = (TaskDecisionImpl) o;

        if (error != that.error) return false;
        if (restartTime != that.restartTime) return false;
        if (exception != null ? !exception.equals(that.exception) : that.exception != null) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (!Arrays.equals(tasks, that.tasks)) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (tasks != null ? Arrays.hashCode(tasks) : 0);
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        result = 31 * result + (error ? 1 : 0);
        result = 31 * result + (int) (restartTime ^ (restartTime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "TaskDecisionImpl{" +
                "uuid=" + uuid +
                ", processId=" + processId +
                ", value=" + value +
                ", tasks=" + Arrays.toString(tasks) +
                ", exception=" + exception +
                ", error=" + error +
                ", restartTime=" + restartTime +
                ", executionTime=" + executionTime +
                "} " + super.toString();
    }
}
