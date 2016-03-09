package ru.taskurotta.service.console.model;

import ru.taskurotta.service.metrics.handler.DatasetSummary;

import java.util.Map;

/**
 */
public class ActorFullVO {

    String id;
    ActorState state;
    private Map<String, DatasetSummary> metrics;
    private int queueSize;
    private long queueDelaySize;
    private long lastPolledTaskEnqueueTime;
    private long currentTimeMillis;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ActorState getState() {
        return state;
    }

    public void setState(ActorState state) {
        this.state = state;
    }

    public void setMetrics(Map<String, DatasetSummary> metrics) {
        this.metrics = metrics;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setQueueDelaySize(long queueDelaySize) {
        this.queueDelaySize = queueDelaySize;
    }

    /**
     * Setter of Last Polled Task Enqueue Time
     * @param lastPolledTaskEnqueueTime
     */
    public void setLastPolledTaskEnqueueTime(long lastPolledTaskEnqueueTime) {
        this.lastPolledTaskEnqueueTime = lastPolledTaskEnqueueTime;
    }

    public Map<String, DatasetSummary> getMetrics() {
        return metrics;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public long getQueueDelaySize() {
        return queueDelaySize;
    }

    public long getLastPolledTaskEnqueueTime() {
        return lastPolledTaskEnqueueTime;
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

    public long getCurrentTimeMillis() {
        return currentTimeMillis;
    }
}
