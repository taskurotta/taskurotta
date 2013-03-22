package ru.taskurotta.oracle.test.domain;

import java.util.Date;

/**
 * User: greg
 */
public class SimpleTask {

    private int taskId;
    private int typeId;
    private Date date;
    private int userId;

    public SimpleTask(int taskId, int typeId, Date date, int userId) {
        this.taskId = taskId;
        this.typeId = typeId;
        this.date = date;
        this.userId = userId;
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
