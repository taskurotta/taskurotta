package ru.taskurotta.service.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.metrics.handler.NumberDataListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Metric for handling periodic data
 * Date: 26.12.13 16:23
 */
public class PeriodicMetric {

    private static final Logger logger = LoggerFactory.getLogger(PeriodicMetric.class);

    private ScheduledExecutorService scheduledExecutorService;
    private int periodSeconds;
    private String metricName;
    private NumberDataListener numberDataListener;
    private int dataSize;

    public PeriodicMetric(String metricName, ScheduledExecutorService scheduledExecutorService, NumberDataListener numberDataListener, int periodSeconds, int dataSize) {
        this.metricName = metricName;
        this.scheduledExecutorService = scheduledExecutorService;
        this.numberDataListener = numberDataListener;
        this.periodSeconds = periodSeconds;
        this.dataSize = dataSize;
    }

    public static interface DatasetValueExtractor {

        /**
         * @return list of all datasets for this metric
         */
        public List<String> getDatasets();

        /**
         * @return value of this metrics's dataset
         */
        public Number getDatasetValue(String dataset);

        /**
         * General value (for all datasets). Could be average value, sum, most possible, or whatever depending on metric
         */
        public Number getGeneralValue(Map<String, Number> datasetsValues);
    }

    public void periodicMark(final DatasetValueExtractor valueExtractor) {

        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> datasets = valueExtractor.getDatasets();
                    Map<String, Number> datasetsValues = null;
                    if (datasets!=null && !datasets.isEmpty()) {
                        datasetsValues = new HashMap<>();
                        for (String dataset : datasets) {
                            Number datasetValue = valueExtractor.getDatasetValue(dataset);
                            datasetsValues.put(dataset, datasetValue);
                            numberDataListener.handleNumberData(metricName, dataset, datasetValue,
                                    System.currentTimeMillis(), dataSize);
                        }
                    }
                    Number generalValue = valueExtractor.getGeneralValue(datasetsValues);
                    if (generalValue!=null) {
                        numberDataListener.handleNumberData(metricName, metricName, generalValue,
                                System.currentTimeMillis(), dataSize);
                    }
                    logger.debug("Flushed metrics data for datasets [{}]", datasets);

                } catch (Throwable e) {
                    logger.error("Periodic metric iteration failed", e);
                }
            }
        }, 0, periodSeconds, TimeUnit.SECONDS);
    }

}
