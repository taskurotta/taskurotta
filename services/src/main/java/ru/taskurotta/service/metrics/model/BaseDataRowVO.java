package ru.taskurotta.service.metrics.model;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 24.10.13 16:49
 */
public class BaseDataRowVO {
    protected final String metricName;
    protected final String dataSetName;
    protected int size = -1;

    /**
     * Размер агрегационного бина в миллисекундах. Индекс слота кольца вычисляется
     * по абсолютному времени данных: (time / binMillis) % size; окно wrap = size * binMillis.
     */
    protected final long binMillis;

    protected volatile long updated = -1L;
    protected volatile long lastActive = -1L;

    public BaseDataRowVO(int size, long binMillis, String metricName, String dataSetName) {
        this.metricName = metricName;
        this.dataSetName = dataSetName;
        this.size = size;
        this.binMillis = binMillis;
    }

    /**
     * Индекс бина по абсолютному времени данных. t и t + окно (size * binMillis) — один и тот же бин.
     */
    protected int binIndex(long time) {
        return (int) ((time / binMillis) % size);
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
