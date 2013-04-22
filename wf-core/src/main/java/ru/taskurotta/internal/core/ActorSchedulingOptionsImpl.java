package ru.taskurotta.internal.core;

import ru.taskurotta.core.ActorSchedulingOptions;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 18:40
 */
public class ActorSchedulingOptionsImpl implements ActorSchedulingOptions {

    private String customId;
    private long startTime = -1;
    private String taskList; // name of task queue/list

    public ActorSchedulingOptionsImpl() {}

    public ActorSchedulingOptionsImpl(String customId, long startTime, String taskList) {
        this.customId = customId;
        this.startTime = startTime;
        this.taskList = taskList;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
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

        ActorSchedulingOptionsImpl that = (ActorSchedulingOptionsImpl) o;

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
        return "ActorSchedulingOptionsImpl{" +
                "customId='" + customId + '\'' +
                ", startTime=" + startTime +
                ", taskList=" + taskList +
                "} " + super.toString();
    }
}
