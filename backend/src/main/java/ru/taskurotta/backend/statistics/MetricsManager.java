package ru.taskurotta.backend.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.metrics.ArrayCheckPoint;
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
public class MetricsManager {

    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);

    private final Map<String, CheckPoint> checkPoints;

    private final ScheduledExecutorService executorService;

    public MetricsManager() {
        this(0, 1, TimeUnit.SECONDS);
    }

    public MetricsManager(long initialDelay, long delay, TimeUnit unit) {
        this.checkPoints = new ConcurrentHashMap<>();
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

    public void mark(String name, String actorId, long period, DataListener dataListener) {
        String key = name + "#" + actorId;

        CheckPoint checkPoint = checkPoints.get(key);

        if (checkPoint == null) {
            synchronized (checkPoints) {
                checkPoint = checkPoints.get(key);

                if (checkPoint == null) {
                    checkPoint = new ArrayCheckPoint(name, actorId, dataListener);
                    checkPoints.put(key, checkPoint);
                }
            }
        }

        checkPoint.mark(period);
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
