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
        super();
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


    public void setMethod(String method) {
        this.method = method;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
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
