package ru.taskurotta.backend.ora.domain;

import java.util.Date;
import java.util.UUID;

import org.springframework.util.StringUtils;

/**
 * User: greg
 */
public class SimpleTask {

    private UUID taskId;
    private Date date;
    private String taskList;
    private int statusId;

    public SimpleTask(UUID taskId, Date date, int statusId, String taskList) {
        this.date = date;
        this.statusId = statusId;
        this.taskId = taskId;
        this.taskList = (StringUtils.hasText(taskList)) ? taskList : "DEF";
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

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }
}
