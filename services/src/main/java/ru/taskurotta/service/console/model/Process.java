package ru.taskurotta.service.console.model;

import java.io.Serializable;
import java.util.UUID;

import ru.taskurotta.transport.model.TaskContainer;

/**
 * POJO representing workflow process
 * User: dimadin
 * Date: 21.05.13 11:09
 */
public class Process implements Serializable {

    public static final int START = 0;
    public static final int FINISH = 1;

    private UUID processId;
    private UUID startTaskId;
    private String customId;
    private long startTime = -1l;
    private long endTime = -1l;
    private int state;
    private String returnValue;
    private TaskContainer startTask;

    public Process() {}

    public Process(TaskContainer startTask) {
        this.processId = startTask.getProcessId();
        this.startTaskId = startTask.getTaskId();
        this.startTime = System.currentTimeMillis();
        this.state = START;
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
}
