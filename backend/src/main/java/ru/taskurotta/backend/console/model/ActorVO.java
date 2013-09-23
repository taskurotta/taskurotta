package ru.taskurotta.backend.console.model;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 18.09.13 12:09
 */
public class ActorVO {

    private String actorId;
    private long lastActive;
    private String queueName;
    private boolean blocked;

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public String toString() {
        return "ActorVO{" +
                "actorId='" + actorId + '\'' +
                ", lastActive=" + lastActive +
                ", queueName='" + queueName + '\'' +
                ", blocked=" + blocked +
                "} ";
    }
}
