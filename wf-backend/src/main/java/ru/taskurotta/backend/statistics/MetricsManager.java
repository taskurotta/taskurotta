package ru.taskurotta.backend.statistics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.metrics.CheckPoint;
import ru.taskurotta.backend.statistics.metrics.Counter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: stukushin
 * Date: 27.08.13
 * Time: 15:12
 */
public class MetricsManager {

    private static final Map<String, CheckPoint> checkPoints = new ConcurrentHashMap<>();
    private static final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public static CheckPoint createCheckPoint(String name, DataListener dataListener) {
        CheckPoint checkPoint = checkPoints.get(name);

        if (checkPoint != null) {
            return checkPoint;
        }

        synchronized (checkPoints) {
            checkPoint = checkPoints.get(name);

            if (checkPoint != null) {
                return checkPoint;
            }

            checkPoint = new CheckPoint(name, dataListener);
            checkPoints.put(name, checkPoint);
        }

        return checkPoint;
    }

    public static Counter createCounter(String name, DataListener dataListener) {
        Counter counter = counters.get(name);

        if (counter != null) {
            return counter;
        }

        synchronized (counters) {
            counter = counters.get(name);

            if (counter != null) {
                return counter;
            }

            counter = new Counter(name, dataListener);
            counters.put(name, counter);
        }

        return counter;
    }

    public static void shutdown() {
        for (Map.Entry<String, CheckPoint> entry : checkPoints.entrySet()) {
            entry.getValue().shutdown();
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            entry.getValue().shutdown();
        }
    }
}
