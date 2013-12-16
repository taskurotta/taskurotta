package ru.taskurotta.service.console.retriever.metrics;

import ru.taskurotta.service.statistics.metrics.data.DataPointVO;

import java.util.Collection;
import java.util.Date;

/**
 * Interface for retrieving metrics data for method invocation: cumber if times method been invoked and mean invocation time
 * User: dimadin
 * Date: 12.09.13 14:17
 */
public interface MetricsMethodDataRetriever {

    /**
     * Retrieves collection of available metric names
     */
    Collection<String> getMetricNames();

    /**
     * Retrieves collection of data sets measured by given metric
     */
    Collection<String> getDataSetsNames(String metricName);

    /**
     * Retrieve aggregated statistic for given metric and dataset
     */
    DataPointVO<Long>[] getCountsForLastHour(String metricName, String datasetName);

    /**
     * Retrieve aggregated statistic for given metric and dataset
     */
    DataPointVO<Long>[] getCountsForLastDay(String metricName, String datasetName);

    /**
     * Retrieve aggregated statistic for given metric and dataset
     */
    DataPointVO<Double>[] getMeansForLastHour(String metricName, String datasetName);

    /**
     * Retrieve aggregated statistic for given metric and dataset
     */
    DataPointVO<Double>[] getMeansForLastDay(String metricName, String datasetName);

    /**
     * @return last update date
     */
    public Date getLastActivityTime(String metricName, String datasetName);

}
