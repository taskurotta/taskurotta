package ru.taskurotta.backend.statistics.metrics.data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    protected AtomicLong updated = new AtomicLong(-1);
    protected AtomicLong lastActive = new AtomicLong(-1);

    public BaseDataRowVO(int size, String metricName, String dataSetName) {
        this.metricName = metricName;
        this.dataSetName = dataSetName;
        this.size = size;
    }

    protected int getPosition() {
        counter.compareAndSet(this.size, 0);//reset counter if exceeded
        return counter.getAndIncrement();
    }

    public String getMetricsName() {
        return metricName;
    }

    public String getDataSetName(){
        return dataSetName;
    }

    public long getUpdated() {
        return this.updated.get();
    }

    public long getLatestActivity() {
        return this.lastActive.get();
    }

}
