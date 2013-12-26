package ru.taskurotta.service.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.statistics.datalistener.DataListener;
import ru.taskurotta.service.statistics.metrics.Metric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Factory for producing Metric objects
 * User: dimadin
 * Date: 12.09.13 17:21
 */
public class MetricsFactory {

    private static final Logger logger = LoggerFactory.getLogger(MetricsFactory.class);

    private Map<String, Metric> metricsCache = new ConcurrentHashMap<>();//stores created Metric instances

    private ScheduledExecutorService executorService;

    /**
     * Defines time resolution for metrics dataset in seonds
     */
    private int dumpPeriod;

    /**
     * Handler for dataset time resolution point (storing/aggregating)
     */
    private DataListener dataListener;

    public MetricsFactory(int dumpPeriod, int dumpingThreads, DataListener dataListener) {
        this.executorService = Executors.newScheduledThreadPool(dumpingThreads);
        this.dumpPeriod = dumpPeriod;
        this.dataListener = dataListener;
    }


    public Metric getInstance(String name, int dumpPeriodMs, DataListener dataListener) {

        Metric metric = metricsCache.get(name);
        if (metric == null) {
            synchronized (metricsCache) {
                metric = metricsCache.get(name);
                if(metric == null) {
                    metric = instantiate(name, dumpPeriodMs, dataListener);
                    metricsCache.put(name, metric);
                }
            }
        }

        return metric;

    }


    public Metric getInstance(String name) {

        return getInstance(name, dumpPeriod, dataListener);
    }


    private Metric instantiate(String name, int periodDelay, final DataListener dataListener) {
        final Metric result = new Metric(name);

        this.executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    result.dump(dataListener);
                } catch (Throwable e) {//must catch all of the exceptions : scheduled pool shuts quietly on exception
                    logger.error("Cannot dump metrics result", e);
                }

            }
        }, 0, periodDelay, TimeUnit.SECONDS);

        return result;
    }

    public void scheduleMetricsTask() {

    }

    public void shutdown() {
        executorService.shutdown();
    }

    public DataListener getDataListener() {
        return dataListener;
    }

}
