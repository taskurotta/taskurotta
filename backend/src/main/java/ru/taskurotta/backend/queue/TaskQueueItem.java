package ru.taskurotta.backend.queue;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Pojo representing task queue item
 * User: dimadin
 * Date: 05.07.13 10:44
 */
public class TaskQueueItem implements Serializable {
    protected UUID taskId;
    protected UUID processId;
    protected long startTime;
    protected long enqueueTime;
    protected String taskList;
    protected Date createdDate;
    protected String queueName;

    public String getQueueName() {
        return queueName;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEnqueueTime() {
        return enqueueTime;
    }

    public void setEnqueueTime(long enqueueTime) {
        this.enqueueTime = enqueueTime;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskQueueItem that = (TaskQueueItem) o;

        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (queueName != null ? !queueName.equals(that.queueName) : that.queueName != null) return false;
        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (taskList != null ? !taskList.equals(that.taskList) : that.taskList != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (taskList != null ? taskList.hashCode() : 0);
        result = 31 * result + (queueName != null ? queueName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskQueueItem{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", startTime=" + startTime +
                ", enqueueTime=" + enqueueTime +
                ", taskList='" + taskList + '\'' +
                ", createdDate=" + createdDate +
                ", queueName='" + queueName + '\'' +
                "} ";
    }
}
