package ru.taskurotta.transport.model;

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
    private int numberOfAttempts;
    private ArgContainer[] args;
    private TaskOptionsContainer options;
    private UUID processId;
    private boolean unsafe;

    public UUID getProcessId() {
        return processId;
    }

    public TaskContainer() {
    }

    public TaskContainer(UUID taskId, UUID processId, String method, String actorId,
                         TaskType type, long startTime, int numberOfAttempts,
                         ArgContainer[] args, TaskOptionsContainer options, boolean unsafe) {
        super();
        this.taskId = taskId;
        this.method = method;
        this.actorId = actorId;
        this.type = type;
        this.startTime = startTime;
        this.numberOfAttempts = numberOfAttempts;
        this.args = args;
        this.options = options;
        this.processId = processId;
        this.unsafe = unsafe;
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

    public int getNumberOfAttempts() {
        return numberOfAttempts;
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

    public void incrementNumberOfAttempts() {
        numberOfAttempts++;
    }

    @Override
    public String toString() {
        return "TaskContainer [taskId=" + taskId
                + ", actorId=" + actorId + ", method=" + method + ", type=" + type
                + ", startTime=" + startTime + ", numberOfAttempts="
                + numberOfAttempts + ", args=" + Arrays.toString(args)
                + ", options=" + options + ", isUnsafe="+ unsafe +"]";
    }
}
