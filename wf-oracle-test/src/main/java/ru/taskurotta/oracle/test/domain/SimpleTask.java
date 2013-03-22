package ru.taskurotta.oracle.test.domain;

import java.util.Date;

/**
 * User: greg
 */
public class SimpleTask {

    private int taskId;
    private int typeId;
    private Date date;
    private int statusId;
    private String actorId;


    public SimpleTask(int taskId, int typeId, Date date, int statusId, String actorId) {
        this.taskId = taskId;
        this.typeId = typeId;
        this.date = date;
        this.statusId = statusId;
        this.actorId = actorId;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }
}
