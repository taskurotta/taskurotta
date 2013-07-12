package ru.taskurotta.backend.checkpoint.model;

import ru.taskurotta.backend.checkpoint.TimeoutType;

import java.util.UUID;

/**
 * Query command for listing Checkpoints from CheckpointService,
 * can contain multiple search/filter/sorting criteria
 */
public class CheckpointQuery {

    //filter by min checkpoint time
    private long minTime;

    //filter by max checkpoint time
    private long maxTime;

    //filter by actorId
    private String actorId;

    //filter by timeout type
    private TimeoutType timeoutType;

    //Filter by taskId
    private UUID taskId;

    //Filter by processId
    private UUID processId;

    public CheckpointQuery(TimeoutType timeoutType, UUID taskId, UUID processId, String actorId, long maxTime, long minTime) {
        this.timeoutType = timeoutType;
        this.taskId = taskId;
        this.processId = processId;
        this.actorId = actorId;
        this.maxTime = maxTime;
        this.minTime = minTime;
    }

    public CheckpointQuery(TimeoutType timeoutType) {
        this(timeoutType, null, null, null, -1, -1);
    }

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }


    public TimeoutType getTimeoutType() {
        return timeoutType;
    }

    public void setTimeoutType(TimeoutType timeoutType) {
        this.timeoutType = timeoutType;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
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

    @Override
    public String toString() {
        return "CheckpointQuery{" +
                "minTime=" + minTime +
                ", maxTime=" + maxTime +
                ", actorId='" + actorId + '\'' +
                ", timeoutType=" + timeoutType +
                ", taskId=" + taskId +
                ", processId=" + processId +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckpointQuery)) return false;

        CheckpointQuery that = (CheckpointQuery) o;

        if (maxTime != that.maxTime) return false;
        if (minTime != that.minTime) return false;
        if (actorId != null ? !actorId.equals(that.actorId) : that.actorId != null) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (timeoutType != that.timeoutType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (minTime ^ (minTime >>> 32));
        result = 31 * result + (int) (maxTime ^ (maxTime >>> 32));
        result = 31 * result + (actorId != null ? actorId.hashCode() : 0);
        result = 31 * result + (timeoutType != null ? timeoutType.hashCode() : 0);
        result = 31 * result + (taskId != null ? taskId.hashCode() : 0);
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        return result;
    }
}
