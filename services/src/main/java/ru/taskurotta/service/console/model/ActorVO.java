package ru.taskurotta.service.console.model;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 18.09.13 12:09
 */
public class ActorVO {

    protected String actorId;
    protected Date lastPoll;
    protected Date lastRelease;

    protected String queueName;
    protected boolean blocked;

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

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public Date getLastPoll() {
        return lastPoll;
    }

    public void setLastPoll(Date lastPoll) {
        this.lastPoll = lastPoll;
    }

    public Date getLastRelease() {
        return lastRelease;
    }

    public void setLastRelease(Date lastRelease) {
        this.lastRelease = lastRelease;
    }

    @Override
    public String toString() {
        return "ActorVO{" +
                "actorId='" + actorId + '\'' +
                ", lastPoll=" + lastPoll +
                ", lastRelease=" + lastRelease +
                ", queueName='" + queueName + '\'' +
                ", blocked=" + blocked +
                "} ";
    }
}
