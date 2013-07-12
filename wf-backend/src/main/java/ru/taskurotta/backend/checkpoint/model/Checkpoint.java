package ru.taskurotta.backend.checkpoint.model;

import ru.taskurotta.backend.checkpoint.TimeoutType;

import java.io.Serializable;
import java.util.UUID;

/**
 * Description for common entity having specified time (checkpoint time).
 * For instance, an expiration time for task having response timeout
 */
public class Checkpoint implements Serializable {

    /**
     * TaskId owning this checkpoint
     */
    protected UUID taskId;

    /**
     * ProcessId of a task owning this checkpoint
     */
    protected UUID processId;

    /**
     * Type of actor for task owning this checkpoint
     */
    protected String actorId;

    /**
     * Type of timeout for which checkpoint is set
     */
    protected TimeoutType timeoutType;

    /**
     * Checkpoint time
     */
    protected long time;

    public Checkpoint(TimeoutType timeoutType, UUID taskId, UUID processId, String actorId, long time) {
        this.timeoutType = timeoutType;
        this.taskId = taskId;
        this.processId = processId;
        this.actorId = actorId;
        this.time = time;
    }

    public Checkpoint() {
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

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public TimeoutType getTimeoutType() {
        return timeoutType;
    }

    public void setTimeoutType(TimeoutType timeoutType) {
        this.timeoutType = timeoutType;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Checkpoint)) return false;

        Checkpoint that = (Checkpoint) o;

        if (time != that.time) return false;
        if (actorId != null ? !actorId.equals(that.actorId) : that.actorId != null) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (timeoutType != that.timeoutType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (actorId != null ? actorId.hashCode() : 0);
        result = 31 * result + (timeoutType != null ? timeoutType.hashCode() : 0);
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Checkpoint{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", actorId='" + actorId + '\'' +
                ", timeoutType=" + timeoutType +
                ", time=" + time +
                "} " + super.toString();
    }
}
