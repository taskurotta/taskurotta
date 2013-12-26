package ru.taskurotta.service.metrics;

/**
 * User: stukushin
 * Date: 10.09.13
 * Time: 18:30
 */
public interface DataListener {

    void handle(String metricName, String datasetName, long count, double mean, long currentTime);

}
