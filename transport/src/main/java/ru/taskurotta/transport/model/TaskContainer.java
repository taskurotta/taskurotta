package ru.taskurotta.transport.model;

import ru.taskurotta.internal.core.TaskType;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:05 PM
 */
public class TaskContainer implements Serializable {

    private UUID taskId;
    private String method;
    private String actorId;
    private TaskType type;
    private long startTime;
    private int errorAttempts;
    private ArgContainer[] args;
    private TaskOptionsContainer options;
    private UUID processId;
    private boolean unsafe;
    private String[] failTypes;

    public UUID getProcessId() {
        return processId;
    }

    public TaskContainer() {
    }

    public TaskContainer(UUID taskId, UUID processId, String method, String actorId,
                         TaskType type, long startTime, int errorAttempts,
                         ArgContainer[] args, TaskOptionsContainer options, boolean unsafe, String[] failTypes) {
        this.taskId = taskId;
        this.method = method;
        this.actorId = actorId;
        this.type = type;
        this.startTime = startTime;
        this.errorAttempts = errorAttempts;
        this.args = args;
        this.options = options;
        this.processId = processId;
        this.unsafe = unsafe;
        this.failTypes = failTypes;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public ArgContainer[] getArgs() {
        return args;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getErrorAttempts() {
        return errorAttempts;
    }

    public TaskOptionsContainer getOptions() {
        return options;
    }

    public String getMethod() {
        return method;
    }

    public String getActorId() {
        return actorId;
    }

    public TaskType getType() {
        return type;
    }

    public boolean isUnsafe() {
        return unsafe;
    }

    public String[] getFailTypes() {
        return failTypes;
    }

    public void incrementErrorAttempts() {
        errorAttempts++;
    }


    public void setArgs(ArgContainer[] args) {
        this.args = args;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskContainer that = (TaskContainer) o;

        if (errorAttempts != that.errorAttempts) return false;
        if (startTime != that.startTime) return false;
        if (unsafe != that.unsafe) return false;
        if (actorId != null ? !actorId.equals(that.actorId) : that.actorId != null) return false;
        if (!Arrays.equals(args, that.args)) return false;
        if (!Arrays.equals(failTypes, that.failTypes)) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        if (options != null ? !options.equals(that.options) : that.options != null) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (actorId != null ? actorId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + errorAttempts;
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (unsafe ? 1 : 0);
        result = 31 * result + (failTypes != null ? Arrays.hashCode(failTypes) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskContainer [taskId=" + taskId
                + ", actorId=" + actorId + ", method=" + method + ", type=" + type
                + ", startTime=" + startTime
                + ", errorAttempts=" + errorAttempts
                + ", args=" + Arrays.toString(args)
                + ", options=" + options
                + ", unsafe=" + unsafe
                + ", failTypes=" + Arrays.toString(failTypes) +"]";
    }
}
