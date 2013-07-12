package ru.taskurotta.backend.ora.domain;

import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * User: greg
 */
public class SimpleTask {

    public static final String DEFAULT_TASK_LIST = "$D";

    private UUID taskId;
    private UUID processId;
    private long startTime;
    private String taskList;
    private int statusId;

    public SimpleTask(UUID taskId, UUID processId, long startTime, int statusId, String taskList) {
        this.startTime = startTime;
        this.statusId = statusId;
        this.taskId = taskId;
        this.processId = processId;
        this.taskList = (StringUtils.hasText(taskList)) ? taskList : DEFAULT_TASK_LIST;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
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

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }
}
