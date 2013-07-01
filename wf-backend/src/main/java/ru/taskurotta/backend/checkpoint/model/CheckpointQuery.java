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

    //filter by entity type
    private String entityType;

    //filter by timeout type
    private TimeoutType timeoutType;

    //Filter by entity entityGuid
    private UUID entityGuid;

    public CheckpointQuery(TimeoutType timeoutType, UUID entityGuid, String entityType, long maxTime, long minTime) {
        this.timeoutType = timeoutType;
        this.entityGuid = entityGuid;
        this.entityType = entityType;
        this.maxTime = maxTime;
        this.minTime = minTime;
    }

    public CheckpointQuery(TimeoutType timeoutType) {
        this(timeoutType, null, null, -1, -1);
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

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public TimeoutType getTimeoutType() {
        return timeoutType;
    }

    public void setTimeoutType(TimeoutType timeoutType) {
        this.timeoutType = timeoutType;
    }

    public UUID getEntityGuid() {
        return entityGuid;
    }

    public void setEntityGuid(UUID entityGuid) {
        this.entityGuid = entityGuid;
    }

    @Override
    public String toString() {
        return "CheckpointQuery [minTime=" + minTime + ", maxTime=" + maxTime
                + ", entityType=" + entityType + ", timeoutType=" + timeoutType
                + ", entityGuid=" + entityGuid + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((entityType == null) ? 0 : entityType.hashCode());
        result = prime * result + (int) (maxTime ^ (maxTime >>> 32));
        result = prime * result + (int) (minTime ^ (minTime >>> 32));
        result = prime * result
                + ((timeoutType == null) ? 0 : timeoutType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CheckpointQuery other = (CheckpointQuery) obj;
        if (entityType == null) {
            if (other.entityType != null)
                return false;
        } else if (!entityType.equals(other.entityType))
            return false;
        if (maxTime != other.maxTime)
            return false;
        if (minTime != other.minTime)
            return false;
        if (timeoutType != other.timeoutType)
            return false;
        return true;
    }

}
