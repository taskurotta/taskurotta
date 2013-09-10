package ru.taskurotta.backend.statistics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.metrics.CheckPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 06.09.13
 * Time: 15:34
 */
public class ActorMetricsManager {

    private final Map<String, Map<String, CheckPoint>> actorCheckPoints = new ConcurrentHashMap<>();
    private final Map<String, Map<String, DataListener>> actorDataListeners = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executorService;

    private Collection<String> names = new CopyOnWriteArraySet<>();

    public ActorMetricsManager(long initialDelay, long delay, TimeUnit unit) {
        this.executorService = Executors.newScheduledThreadPool(1);

        this.executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Collection<Map<String, CheckPoint>> values = new ArrayList<>(actorCheckPoints.values());

                for (Map<String, CheckPoint> item: values) {

                    Collection<CheckPoint> checkPoints = item.values();

                    for (CheckPoint checkPoint : checkPoints) {
                        checkPoint.dump();
                    }
                }
            }
        }, initialDelay, delay, unit);
    }

    public void mark(String actorId, String name, long period, DataListener dataListener) {

        CheckPoint checkPoint;

        if (!actorCheckPoints.containsKey(actorId)) {
            synchronized (actorCheckPoints) {
                if (!actorCheckPoints.containsKey(actorId)) {
                    actorCheckPoints.put(actorId, new HashMap<String, CheckPoint>());
                }
            }
        }

        checkPoint = actorCheckPoints.get(actorId).get(name);

        if (checkPoint == null) {
            synchronized (actorCheckPoints) {
                checkPoint = actorCheckPoints.get(actorId).get(name);

                if (checkPoint == null) {

                    checkPoint = new CheckPoint(name, dataListener);

                    actorCheckPoints.get(actorId).put(name, checkPoint);

                    if (!actorDataListeners.containsKey(actorId)) {
                        actorDataListeners.put(actorId, new HashMap<String, DataListener>());
                    }

                    actorDataListeners.get(actorId).put(name, dataListener);

                    names.add(name);
                }
            }
        }

        checkPoint.mark(period);
    }

    public Collection<String> getActorIds() {
        return actorCheckPoints.keySet();
    }

    public Collection<String> getNames() {
        return names;
    }

    public long[] getHourCounts(String actorId, String name) {

        DataListener dataListener = getDataListener(actorId, name);

        if (dataListener == null) {
            return new long[0];
        }

        return dataListener.getHourCounts();
    }

    public long[] getDayCounts(String actorId, String name) {
        DataListener dataListener = getDataListener(actorId, name);

        if (dataListener == null) {
            return new long[0];
        }

        return dataListener.getDayCounts();
    }

    public double[] getHourMeans(String actorId, String name) {
        DataListener dataListener = getDataListener(actorId, name);

        if (dataListener == null) {
            return new double[0];
        }

        return dataListener.getHourMeans();
    }

    public double[] getDayMeans(String actorId, String name) {
        DataListener dataListener = getDataListener(actorId, name);

        if (dataListener == null) {
            return new double[0];
        }

        return dataListener.getDayMeans();
    }

    private DataListener getDataListener(String actorId, String name) {
        if (actorId == null) {
            return null;
        }

        if (name == null) {
            return null;
        }

        if (!actorDataListeners.containsKey(actorId)) {
            return null;
        }

        Map<String, DataListener> dataListeners = actorDataListeners.get(actorId);

        if (dataListeners == null) {
            return null;
        }

        if (!dataListeners.containsKey(name)) {
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
