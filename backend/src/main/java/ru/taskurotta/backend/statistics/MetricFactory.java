package ru.taskurotta.backend.statistics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.metrics.Metric;

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
public class MetricFactory {

    private Map<String, Metric> metricsCache = new ConcurrentHashMap<>();//stores created Metric instances

    private ScheduledExecutorService executorService;

    private int dumpPeriod;//defines time resolution for metrics dataset

    private DataListener dataListener;//handler for dataset time resolution point (storing/aggregating)

    public MetricFactory(int dumpPeriod, int dumpingThreads, DataListener dataListener) {
        this.executorService = Executors.newScheduledThreadPool(dumpingThreads);
        this.dumpPeriod = dumpPeriod;
        this.dataListener = dataListener;
    }

    public Metric getInstance(String name) {
        Metric result = metricsCache.get(name);

        if(result == null) {
            synchronized (metricsCache) {
                if(!metricsCache.containsKey(name)) {
                    result = instantiate(name, dumpPeriod);
                    metricsCache.put(name, result);
                }
            }
        }

        return result;
    }


    private Metric instantiate(String name, int periodDelay) {
        final Metric result = new Metric(name);

        this.executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                result.dump(dataListener);
            }
        }, 0, periodDelay, TimeUnit.SECONDS);

        return result;
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
