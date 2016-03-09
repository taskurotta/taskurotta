package ru.taskurotta.service.metrics.model;

/**
 */
public class DataRowSummary {
    private double mean;
    private long count;
    private long timeMin;
    private long timeMax;

    public void setMean(double mean) {
        this.mean = mean;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void setTimeMin(long timeMin) {
        this.timeMin = timeMin;
    }

    public void setTimeMax(long timeMax) {
        this.timeMax = timeMax;
    }

    public double getMean() {
        return mean;
    }

    public long getCount() {
        return count;
    }

    public long getTimeMin() {
        return timeMin;
    }

    public long getTimeMax() {
        return timeMax;
    }
}
