package ru.taskurotta.backend.storage.model;

import java.util.Arrays;
import java.util.UUID;

import ru.taskurotta.core.TaskTarget;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:05 PM
 */
public class TaskContainer {

    private UUID taskId;
    private TaskTarget target;
    private long startTime;
    private int numberOfAttempts;
    private ArgContainer[] args;
    private TaskOptionsContainer options;

    public TaskContainer() {
    }

    public TaskContainer(UUID taskId, TaskTarget target, long startTime, int numberOfAttempts, ArgContainer[] args,
                         TaskOptionsContainer options) {
        this.taskId = taskId;
        this.target = target;
        this.startTime = startTime;
        this.numberOfAttempts = numberOfAttempts;
        this.args = args;
        this.options = options;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public TaskTarget getTarget() {
        return target;
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

    @Override
    public String toString() {
        return "TaskContainer{" +
                "taskId=" + taskId +
                ", target=" + target +
                ", startTime=" + startTime +
                ", numberOfAttempts=" + numberOfAttempts +
                ", args=" + (args == null ? null : Arrays.asList(args)) +
                ", options=" + options +
                '}';
    }
}
