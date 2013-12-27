package ru.taskurotta.service.metrics.handler;

import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.MetricsFactory;
import ru.taskurotta.service.metrics.PeriodicMetric;
import ru.taskurotta.service.metrics.PeriodicMetric.DatasetValueExtractor;

import java.util.List;
import java.util.Map;

/**
 * Metrics handler for periodic check of GC service queue size
 * Date: 27.12.13 18:23
 */
public class GcMetricHandler {

    public GcMetricHandler(MetricsFactory metricsFactory, final GarbageCollectorService garbageCollectorService, int periodSeconds) {

        PeriodicMetric gcQueueSizeMetric = metricsFactory.getPeriodicInstance(MetricName.GARBAGE_COLLECTOR_QUEUE_SIZE.getValue(), periodSeconds);
        gcQueueSizeMetric.periodicMark(new DatasetValueExtractor() {
            @Override
            public List<String> getDatasets() {//no datasets
                return null;
            }

            @Override
            public Number getDatasetValue(String dataset) {//no datasets no value
                return null;
            }

            @Override
            public Number getGeneralValue(Map<String, Number> datasetsValues) {
                return garbageCollectorService.getCurrentSize();
            }
        });

    }


}
