package ru.taskurotta.service.console.model;

import ru.taskurotta.transport.model.TaskContainer;

import java.io.Serializable;
import java.util.UUID;

/**
 * POJO representing workflow process
 * User: dimadin
 * Date: 21.05.13 11:09
 */
public class Process implements Serializable {

    public static final int START = 0;
    public static final int FINISH = 1;
    public static final int BROKEN = 2;

    protected UUID processId;
    protected UUID startTaskId;
    protected String customId;
    protected long startTime = -1l;
    protected long endTime = -1l;
    protected int state;
    protected String returnValue;
    protected TaskContainer startTask;

    public Process() {}

    public Process(TaskContainer startTask) {
        this.processId = startTask.getProcessId();
        this.startTaskId = startTask.getTaskId();
        this.startTime = System.currentTimeMillis();
        this.state = START;
        this.startTask = startTask;
    }

    public Process(TaskContainer startTask, String customId, long startTime, long endTime, int state, String returnValue) {
        this.processId = startTask.getProcessId();
        this.startTaskId = startTask.getTaskId();
        this.customId = customId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.state = state;
        this.returnValue = returnValue;
        this.startTask = startTask;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public UUID getStartTaskId() {
        return startTaskId;
    }

    public void setStartTaskId(UUID startTaskId) {
        this.startTaskId = startTaskId;
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

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(String returnValue) {
        this.returnValue = returnValue;
    }

    public TaskContainer getStartTask() {
        return startTask;
    }

    public void setStartTask(TaskContainer startTask) {
        this.startTask = startTask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Process process = (Process) o;

        if (endTime != process.endTime) return false;
        if (startTime != process.startTime) return false;
        if (state != process.state) return false;
        if (customId != null ? !customId.equals(process.customId) : process.customId != null) return false;
        if (!processId.equals(process.processId)) return false;
        if (returnValue != null ? !returnValue.equals(process.returnValue) : process.returnValue != null) return false;
        if (!startTask.equals(process.startTask)) return false;
        if (!startTaskId.equals(process.startTaskId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = processId.hashCode();
        result = 31 * result + startTaskId.hashCode();
        result = 31 * result + (customId != null ? customId.hashCode() : 0);
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (int) (endTime ^ (endTime >>> 32));
        result = 31 * result + state;
        result = 31 * result + (returnValue != null ? returnValue.hashCode() : 0);
        result = 31 * result + startTask.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Process{" +
                "processId=" + processId +
                ", startTaskId=" + startTaskId +
                ", customId='" + customId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", state=" + state +
                ", returnValue='" + returnValue + '\'' +
                ", startTask=" + startTask +
                '}';
    }
}
