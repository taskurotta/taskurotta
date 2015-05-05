package ru.taskurotta.dropwizard.resources.console.schedule.model;

import ru.taskurotta.internal.core.TaskType;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created on 21.04.2015.
 */
public class TaskCommand implements Serializable {

    protected String actorId;
    protected String method;
    protected TaskType taskType;
    protected ArgVO[] args;
    protected String taskList;

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public ArgVO[] getArgs() {
        return args;
    }

    public void setArgs(ArgVO[] args) {
        this.args = args;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }

    @Override
    public String toString() {
        return "TaskDescriptor{" +
                "actorId='" + actorId + '\'' +
                ", method='" + method + '\'' +
                ", taskType=" + taskType +
                ", args=" + Arrays.toString(args) +
                ", taskList='" + taskList + '\'' +
                '}';
    }
}
