package ru.taskurotta.service.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.metrics.handler.DataListener;
import ru.taskurotta.service.metrics.handler.NumberDataListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
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

    private NumberDataListener numberDataListener;

    public MetricsFactory(int dumpPeriod, int dumpingThreads, DataListener dataListener, NumberDataListener numberDataListener) {
        this.executorService = Executors.newScheduledThreadPool(dumpingThreads, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("Metrics-job-" + counter++);
                return thread;
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                executorService.shutdown();
            }
        });
        this.dumpPeriod = dumpPeriod;
        this.dataListener = dataListener;
        this.numberDataListener = numberDataListener;
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

    public PeriodicMetric getPeriodicInstance(String name, int periodSeconds) {

        //number of points to cover 24 hours period.
        int dataPointsCount = TimeConstants.SECONDS_IN_24_HOURS/periodSeconds;

        return new PeriodicMetric(name, executorService, numberDataListener, periodSeconds, dataPointsCount);
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

    public void shutdown() {
        executorService.shutdown();
    }

    public DataListener getDataListener() {
        return dataListener;
    }

}
