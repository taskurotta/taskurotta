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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InterruptedTask that = (InterruptedTask) o;

        if (time != that.time) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (actorId != null ? !actorId.equals(that.actorId) : that.actorId != null) return false;
        if (starterId != null ? !starterId.equals(that.starterId) : that.starterId != null) return false;
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null) return false;
        return !(errorClassName != null ? !errorClassName.equals(that.errorClassName) : that.errorClassName != null);

    }

    @Override
    public int hashCode() {
        int result = processId != null ? processId.hashCode() : 0;
        result = 31 * result + (taskId != null ? taskId.hashCode() : 0);
        result = 31 * result + (actorId != null ? actorId.hashCode() : 0);
        result = 31 * result + (starterId != null ? starterId.hashCode() : 0);
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
        result = 31 * result + (errorClassName != null ? errorClassName.hashCode() : 0);
        return result;
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
