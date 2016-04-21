package ru.taskurotta.service.metrics.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 24.10.13 16:49
 */
public class BaseDataRowVO {
    protected final String metricName;
    protected final String dataSetName;
    protected int size = -1;

    protected final AtomicInteger counter = new AtomicInteger(0);
    protected volatile long updated = -1L;
    protected volatile long lastActive = -1L;

    public BaseDataRowVO(int size, String metricName, String dataSetName) {
        this.metricName = metricName;
        this.dataSetName = dataSetName;
        this.size = size;
    }

    protected int getPosition() {
        counter.compareAndSet(this.size, 0);//reset counter if exceeded
        return counter.getAndIncrement();
    }

    protected int getCurrentPositionOnly() {
        return counter.get();
    }

    public String getMetricsName() {
        return metricName;
    }

    public String getDataSetName(){
        return dataSetName;
    }

    public long getUpdated() {
        return this.updated;
    }

    public long getLatestActivity() {
        return this.lastActive;
    }

}
