package ru.taskurotta.dropwizard.resources.console.schedule.model;

import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.schedule.JobConstants;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created on 17.07.2014.
 */
public class CreateJobCommand implements Serializable {

    private String name;
    private String cron;
    private int queueLimit = -1;
    private int maxErrors = JobConstants.DEFAULT_MAX_CONSEQUENTIAL_ERRORS;

    private String actorId;
    private String method;
    private TaskType taskType;
    private ArgVO[] args;

    private String taskList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public int getQueueLimit() {
        return queueLimit;
    }

    public void setQueueLimit(int queueLimit) {
        this.queueLimit = queueLimit;
    }

    public int getMaxErrors() {
        return maxErrors;
    }

    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CreateJobCommand that = (CreateJobCommand) o;

        if (maxErrors != that.maxErrors) return false;
        if (queueLimit != that.queueLimit) return false;
        if (actorId != null ? !actorId.equals(that.actorId) : that.actorId != null) return false;
        if (!Arrays.equals(args, that.args)) return false;
        if (cron != null ? !cron.equals(that.cron) : that.cron != null) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (taskList != null ? !taskList.equals(that.taskList) : that.taskList != null) return false;
        if (taskType != that.taskType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (cron != null ? cron.hashCode() : 0);
        result = 31 * result + queueLimit;
        result = 31 * result + maxErrors;
        result = 31 * result + (actorId != null ? actorId.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (taskType != null ? taskType.hashCode() : 0);
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        result = 31 * result + (taskList != null ? taskList.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CreateJobCommand{" +
                "name='" + name + '\'' +
                ", cron='" + cron + '\'' +
                ", queueLimit=" + queueLimit +
                ", maxErrors=" + maxErrors +
                ", actorId='" + actorId + '\'' +
                ", method='" + method + '\'' +
                ", taskType=" + taskType +
                ", args=" + Arrays.toString(args) +
                ", taskList='" + taskList + '\'' +
                '}';
    }

}
