package ru.taskurotta.service.config.model;

import java.io.Serializable;

/**
 * Server-side preferences of registered actor
 */
public class ActorPreferences implements Serializable {

    private String id;
    private boolean blocked = false;
    private String queueName;
    private long keepTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public long getKeepTime() {
        return keepTime;
    }

    public void setKeepTime(long keepTime) {
        this.keepTime = keepTime;
    }

    @Override
    public String toString() {
        return "ActorPreferences{" +
                "id='" + id + '\'' +
                ", blocked=" + blocked +
                ", queueName='" + queueName + '\'' +
                ", keepTime=" + keepTime +
                '}';
    }
}
