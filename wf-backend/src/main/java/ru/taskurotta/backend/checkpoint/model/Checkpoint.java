package ru.taskurotta.backend.checkpoint.model;

import java.util.UUID;

/**
 * Description for common entity having specified time (checkpoint time).
 * For instance, an expiration time for task having response timeout
 */
public class Checkpoint {

    //Entity guid
    private UUID guid;

    //Type of object having this guid
    private String type;

    //Checkpoint time
    private long time;

    public UUID getGuid() {
        return guid;
    }
    public void setGuid(UUID guid) {
        this.guid = guid;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Checkpoint [guid=" + guid + ", type=" + type + ", time=" + time
                + "]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((guid == null) ? 0 : guid.hashCode());
        result = prime * result + (int) (time ^ (time >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        if (guid == null) {
            if (other.guid != null)
                return false;
        } else if (!guid.equals(other.guid))
            return false;
        if (time != other.time)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

}
