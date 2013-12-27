package ru.taskurotta.service.metrics.handler;

import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.MetricsFactory;
import ru.taskurotta.service.metrics.PeriodicMetric;
import ru.taskurotta.service.metrics.PeriodicMetric.DatasetValueExtractor;

import java.util.List;
import java.util.Map;

/**
 * Metrics handler for periodic check of OperationExecutor size
 * Date: 27.12.13 15:56
 */
public class OperationMetricsHandler {

    public OperationMetricsHandler (MetricsFactory metricsFactory, final OperationExecutor operationExecutor, int periodSeconds) {
        PeriodicMetric operationExecutorSizeMetric = metricsFactory.getPeriodicInstance(MetricName.OPERATION_EXECUTOR_SIZE.getValue(), periodSeconds);

        operationExecutorSizeMetric.periodicMark(new DatasetValueExtractor() {

            @Override
            public List<String> getDatasets() {//no dataset, jus a single general value
                return null;
            }

            @Override
            public Number getDatasetValue(String dataset) {//no datasets no value
                return null;
            }

            @Override
            public Number getGeneralValue(Map<String, Number> datasetsValues) {
                return operationExecutor.size();
            }
        });
    }


}
