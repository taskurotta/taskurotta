package ru.taskurotta.backend.checkpoint.model;

import java.util.UUID;

import ru.taskurotta.backend.checkpoint.TimeoutType;

/**
 * Description for common entity having specified time (checkpoint time).
 * For instance, an expiration time for task having response timeout
 */
public class Checkpoint {

    //Entity guid
    private UUID guid;

    //Type of entity quid field references to
    private String entityType;

    //Type of timeout for which checkpoint is set
    private TimeoutType timeoutType;

    //Checkpoint time
    private long time;

    public Checkpoint(TimeoutType timeoutType, UUID guid, String entityType, long time) {
        this.guid = guid;
        this.time = time;
        this.entityType = entityType;
        this.timeoutType = timeoutType;
    }

    public Checkpoint(TimeoutType timeoutType, UUID guid, long time) {
        this(timeoutType, guid, null, time);
    }

    public Checkpoint() {
    }

    public UUID getGuid() {
        return guid;
    }
    public void setGuid(UUID guid) {
        this.guid = guid;
    }

    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
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

    @Override
    public String toString() {
        return "Checkpoint [guid=" + guid + ", entityType=" + entityType
                + ", timeoutType=" + timeoutType + ", time=" + time + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((entityType == null) ? 0 : entityType.hashCode());
        result = prime * result + ((guid == null) ? 0 : guid.hashCode());
        result = prime * result + (int) (time ^ (time >>> 32));
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
        Checkpoint other = (Checkpoint) obj;
        if (entityType == null) {
            if (other.entityType != null)
                return false;
        } else if (!entityType.equals(other.entityType))
            return false;
        if (guid == null) {
            if (other.guid != null)
                return false;
        } else if (!guid.equals(other.guid))
            return false;
        if (time != other.time)
            return false;
        if (timeoutType != other.timeoutType)
            return false;
        return true;
    }

}
