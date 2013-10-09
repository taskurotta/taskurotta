package ru.taskurotta.backend.console.model;

import java.io.Serializable;
import java.util.Date;

/**
 * User: dimadin
 * Date: 07.10.13 14:25
 */
public class MetricsStatDataVO implements Serializable {

    private String metricName;
    private String datasetName;

    private long totalCountsHour = 0l;
    private long totalCountsDay = 0l;

    private double meanTimeHour = 0l;
    private double meanTimeDay = 0l;

    private Date lastActivity;

    public long getTotalCountsHour() {
        return totalCountsHour;
    }

    public void setTotalCountsHour(long totalCountsHour) {
        this.totalCountsHour = totalCountsHour;
    }

    public long getTotalCountsDay() {
        return totalCountsDay;
    }

    public void setTotalCountsDay(long totalCountsDay) {
        this.totalCountsDay = totalCountsDay;
    }

    public double getMeanTimeHour() {
        return meanTimeHour;
    }

    public void setMeanTimeHour(double meanTimeHour) {
        this.meanTimeHour = meanTimeHour;
    }

    public double getMeanTimeDay() {
        return meanTimeDay;
    }

    public void setMeanTimeDay(double meanTimeDay) {
        this.meanTimeDay = meanTimeDay;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    @Override
    public String toString() {
        return "MetricsStatDataVO{" +
                "metricName='" + metricName + '\'' +
                ", datasetName='" + datasetName + '\'' +
                ", totalCountsHour=" + totalCountsHour +
                ", totalCountsDay=" + totalCountsDay +
                ", meanTimeHour=" + meanTimeHour +
                ", meanTimeDay=" + meanTimeDay +
                ", lastActivity=" + lastActivity +
                "} ";
    }
}
