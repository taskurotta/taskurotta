package ru.taskurotta.core;

/**
 * Date: 15.04.13 16:45
 */
public class TaskConfig {

    private String customId;
    private long startTime = -1;
    private String taskList; // name of task queue/list

    public String getCustomId() {
        return customId;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getTaskList() {
        return taskList;
    }

    public TaskConfig setCustomId(String customId) {
        this.customId = customId;
        return this;
    }

    public TaskConfig setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public TaskConfig setTaskList(String taskList) {
        this.taskList = taskList;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskConfig that = (TaskConfig) o;

        if (startTime != that.startTime) return false;
        if (customId != null ? !customId.equals(that.customId) : that.customId != null) return false;
        if (taskList != null ? !taskList.equals(that.taskList) : that.taskList != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = customId != null ? customId.hashCode() : 0;
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (taskList != null ? taskList.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActorSchedulingOptions{" +
                "customId='" + customId + '\'' +
                ", startTime=" + startTime +
                ", taskList='" + taskList + '\'' +
                '}';
    }
}
