package ru.taskurotta.backend.ora.domain;

import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

/**
 * User: greg
 */
public class SimpleTask {

    public static final String DEFAULT_TASK_LIST = "$D";

    private UUID taskId;
    private Date date;
    private String taskList;
    private int statusId;

    public SimpleTask(UUID taskId, Date date, int statusId, String taskList) {
        this.date = date;
        this.statusId = statusId;
        this.taskId = taskId;
        this.taskList = (StringUtils.hasText(taskList)) ? taskList : DEFAULT_TASK_LIST;
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
