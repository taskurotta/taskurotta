package ru.taskurotta.service.metrics.handler;

import ru.taskurotta.service.metrics.model.DataRowSummary;

/**
 */
public class DatasetSummary {

    String metricName;
    String datasetName;
    private long lastTime;
    private DataRowSummary hour;
    private DataRowSummary day;

    public DatasetSummary(String metricName, String datasetName) {
        this.metricName = metricName;
        this.datasetName = datasetName;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public void setHour(DataRowSummary hour) {
        this.hour = hour;
    }

    public void setDay(DataRowSummary day) {
        this.day = day;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public long getLastTime() {
        return lastTime;
    }

    public DataRowSummary getHour() {
        return hour;
    }

    public DataRowSummary getDay() {
        return day;
    }
}
