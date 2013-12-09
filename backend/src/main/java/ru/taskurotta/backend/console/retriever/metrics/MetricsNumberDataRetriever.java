package ru.taskurotta.backend.console.retriever.metrics;

import ru.taskurotta.backend.statistics.metrics.data.DataPointVO;

import java.util.Collection;
import java.util.Date;

/**
 * User: dimadin
 * Date: 25.10.13 10:14
 */
public interface MetricsNumberDataRetriever {

    /**
     * Retrieves collection of available metric names
     */
    Collection<String> getNumberMetricNames();

    /**
     * Retrieves collection of data sets measured by given metric
     */
    Collection<String> getNumberDataSets(String metricName);

    /**
     * Retrieve statistic for given metric and dataset
     */
    DataPointVO<Number>[] getData(String metricName, String datasetName);

    /**
     * @return last update date
     */
    Date getLastActivityTime(String metricName, String datasetName);

    /**
     * @return last measured value
     */
    Number getLastValue(String metricName, String datasetName);

}
