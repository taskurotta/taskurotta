package ru.taskurotta.schedule;

import ru.taskurotta.transport.model.TaskContainer;

import java.io.Serializable;

/**
 *
 * User: dimadin
 * Date: 23.09.13 10:31
 */
public class JobVO implements Serializable {
    protected long id = -1;
    protected String name;
    protected String cron;
    protected TaskContainer task;
    protected boolean allowDuplicates = true;
    protected int status = JobConstants.STATUS_UNDEFINED;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public TaskContainer getTask() {
        return task;
    }

    public void setTask(TaskContainer task) {
        this.task = task;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isAllowDuplicates() {
        return allowDuplicates;
    }

    public void setAllowDuplicates(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    @Override
    public String toString() {
        return "JobVO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", cron='" + cron + '\'' +
                ", task=" + task +
                ", allowDuplicates=" + allowDuplicates +
                ", status=" + status +
                "} ";
    }

}
