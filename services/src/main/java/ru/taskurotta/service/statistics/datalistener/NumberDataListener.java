package ru.taskurotta.service.statistics.datalistener;

/**
 * DataListener for metrics data representing one simple Number at a time
 * For example, periodic checks of a queue size value
 *
 * User: dimadin
 * Date: 25.10.13 10:27
 */
public interface NumberDataListener {

    void handleNumberData(String metricName, String datasetName, Number value, long currentTime, int maxPoints);

}
