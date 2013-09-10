package ru.taskurotta.backend.statistics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.metrics.CheckPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 27.08.13
 * Time: 15:12
 */
public class GeneralMetricsManager {

    private final Map<String, CheckPoint> checkPoints;
    private final Map<String, DataListener> dataListeners;

    private final ScheduledExecutorService executorService;

    public GeneralMetricsManager() {
        this(0, 1, TimeUnit.SECONDS);
    }

    public GeneralMetricsManager(long initialDelay, long delay, TimeUnit unit) {
        this.checkPoints = new ConcurrentHashMap<>();
        this.dataListeners = new ConcurrentHashMap<>();

        this.executorService = Executors.newSingleThreadScheduledExecutor();

        this.executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Collection<CheckPoint> values = new ArrayList<>(checkPoints.values());

                for (CheckPoint checkPoint : values) {
                    checkPoint.dump();
                }
            }
        }, initialDelay, delay, unit);
    }

    public CheckPoint mark(String name, long period, DataListener dataListener) {
        CheckPoint checkPoint = checkPoints.get(name);

        if (checkPoint == null) {
            synchronized (checkPoints) {
                checkPoint = checkPoints.get(name);

                if (checkPoint == null) {
                    checkPoint = new CheckPoint(name, dataListener);

                    checkPoints.put(name, checkPoint);
                    dataListeners.put(name, dataListener);
                }
            }
        }

        checkPoint.mark(period);

        return checkPoint;
    }

    public Collection<String> getNames() {
        return checkPoints.keySet();
    }

    public long[] getHourCounts(String name) {
        DataListener dataListener = getDataListener(name);

        if (dataListener == null) {
            return new long[0];
        }

        return dataListener.getHourCounts();
    }

    public long[] getDayCounts(String name) {
        DataListener dataListener = getDataListener(name);

        if (dataListener == null) {
            return new long[0];
        }

        return dataListener.getDayCounts();
    }

    public double[] getHourMeans(String name) {
        DataListener dataListener = getDataListener(name);

        if (dataListener == null) {
            return new double[0];
        }

        return dataListener.getHourMeans();
    }

    public double[] getDayMeans(String name) {
        DataListener dataListener = getDataListener(name);

        if (dataListener == null) {
            return new double[0];
        }

        return dataListener.getDayMeans();
    }

    private DataListener getDataListener(String name) {
        if (name == null) {
            return null;
        }

        if (!checkPoints.containsKey(name)) {
            return null;
        }

        return dataListeners.get(name);
    }

    public void shutdown() {
        executorService.shutdown();

        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            // do nothing
        }
    }
}
