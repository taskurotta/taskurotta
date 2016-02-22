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


    protected UUID taskId;
    protected UUID processId;
    protected UUID pass;
    protected Object value;
    protected Task[] tasks;
    protected Throwable exception;
    protected boolean error;
    protected long restartTime = NO_RESTART;
    protected long executionTime;

    protected TaskDecisionImpl() {
    }

    public TaskDecisionImpl(UUID taskId, UUID processId, UUID pass, Object value, Task[] tasks, long executionTime) {
        this.taskId = taskId;
        this.processId = processId;
        this.pass = pass;
        this.taskId = taskId;
        this.value = value;
        this.tasks = tasks;
        this.error = false;
        this.executionTime = executionTime;
    }

    public TaskDecisionImpl(UUID taskId, UUID processId, UUID pass, Throwable exception, Task[] tasks) {
        this.taskId = taskId;
        this.processId = processId;
        this.pass = pass;
        this.exception = exception;
        this.tasks = tasks;
        this.error = exception != null;
    }

    public TaskDecisionImpl(UUID taskId, UUID processId, UUID pass, Throwable exception, Task[] tasks, long restartTime) {
        this.taskId = taskId;
        this.processId = processId;
        this.pass = pass;
        this.exception = exception;
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
        return taskId;
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
    public UUID getPass() {
        return pass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskDecisionImpl that = (TaskDecisionImpl) o;

        if (error != that.error) return false;
        if (restartTime != that.restartTime) return false;
        if (exception != null ? !exception.equals(that.exception) : that.exception != null) return false;
        if (pass != null ? !pass.equals(that.pass) : that.pass != null) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (!Arrays.equals(tasks, that.tasks)) return false;
        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (pass != null ? pass.hashCode() : 0);
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
                "taskId=" + taskId +
                ", processId=" + processId +
                ", pass=" + pass +
                ", value=" + value +
                ", tasks=" + Arrays.toString(tasks) +
                ", exception=" + exception +
                ", error=" + error +
                ", restartTime=" + restartTime +
                ", executionTime=" + executionTime +
                '}';
    }
}