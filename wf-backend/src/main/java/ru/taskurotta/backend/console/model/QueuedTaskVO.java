package ru.taskurotta.backend.console.model;

import java.util.UUID;

/**
 * Pojo representing queued task object
 * User: dimadin
 * Date: 22.05.13 15:19
 */
public class QueuedTaskVO {

    private UUID id;
    private String taskList;
    private long startTime;
    private long insertTime;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(long insertTime) {
        this.insertTime = insertTime;
    }

    @Override
    public String toString() {
        return "QueuedTaskVO{" +
                "id=" + id +
                ", taskList='" + taskList + '\'' +
                ", startTime=" + startTime +
                ", insertTime=" + insertTime +
                '}';
    }

}
