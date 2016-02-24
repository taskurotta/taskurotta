package ru.taskurotta.service.console.model;

import ru.taskurotta.service.metrics.model.QueueBalanceVO;

import java.io.Serializable;

//ActorVO with extended features for console UI
public class ActorExtVO extends ActorVO implements Serializable {

    protected QueueBalanceVO queueState;
    protected  double hourRate = 0d;
    protected double dayRate = 0d;

    public ActorExtVO(ActorVO actor) {
        this.id = actor.getId();
        this.blocked = actor.isBlocked();
        this.queueName = actor.getQueueName();
        this.lastPoll = actor.getLastPoll();
        this.lastRelease = actor.getLastRelease();
    }

    public QueueBalanceVO getQueueState() {
        return queueState;
    }

    public double getHourRate() {
        return hourRate;
    }

    public double getDayRate() {
        return dayRate;
    }

    public void setQueueState(QueueBalanceVO queueState) {
        this.queueState = queueState;
    }

    public void setHourRate(double hourRate) {
        this.hourRate = hourRate;
    }

    public void setDayRate(double dayRate) {
        this.dayRate = dayRate;
    }

    @Override
    public String toString() {
        return "ActorExtVO {" +
                "queueState=" + queueState +
                ", hourRate=" + hourRate +
                ", dayRate=" + dayRate +
                "}";
    }
}
