package ru.taskurotta.service.console.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created on 19.03.2015.
 */
public class InterruptedTask implements Serializable {

    protected UUID processId;
    protected UUID taskId;
    protected String actorId;
    protected String starterId;
    protected long time;
    protected String errorMessage;
    protected String errorClassName;

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorClassName() {
        return errorClassName;
    }

    public void setErrorClassName(String errorClassName) {
        this.errorClassName = errorClassName;
    }

    public String getStarterId() {
        return starterId;
    }

    public void setStarterId(String starterId) {
        this.starterId = starterId;
    }

    @Override
    public String toString() {
        return "InterruptedTask{" +
                "processId=" + processId +
                ", taskId=" + taskId +
                ", actorId='" + actorId + '\'' +
                ", starterId='" + starterId + '\'' +
                ", time=" + time +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorClassName='" + errorClassName + '\'' +
                '}';
    }
}
