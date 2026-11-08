package ru.taskurotta.service.metrics;

import org.junit.Test;
import ru.taskurotta.service.metrics.handler.DataListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Ключ создаётся при первой отметке (один раз); dump обходит
 * ВСЕ ключи, включая idle (без активности с прошлого дампа).
 */
public class MetricTest {

    /** Ручной стаб-слушатель: собирает все дамп-вызовы по датасетам. */
    private static final class RecordingListener implements DataListener {
        final List<String> datasets = new ArrayList<String>();
        final Map<String, Long> counts = new HashMap<String, Long>();
        final Map<String, Double> means = new HashMap<String, Double>();

        @Override
        public void handle(String metricName, String datasetName, long count, double mean, long currentTime) {
            datasets.add(datasetName);
            counts.put(datasetName, count);
            means.put(datasetName, mean);
        }
    }

    @SuppressWarnings("unchecked")
    private static CheckPoint getCheckpoint(Metric metric, String dataSetName) throws Exception {
        Field field = Metric.class.getDeclaredField("dataSets");
        field.setAccessible(true);
        Map<String, CheckPoint> dataSets = (Map<String, CheckPoint>) field.get(metric);
        return dataSets.get(dataSetName);
    }

    @Test
    public void mark_CreatesKeyOnce_AndDumpVisitsAllKeys() throws Exception {
        final Metric metric = new Metric("poll");
        final String actorKey = "ru.taskurotta.actors.TestActor:1.0";
        final String idleKey = "ru.taskurotta.actors.IdleActor:1.0";

        // 1. Ключ создаётся при ПЕРВОЙ отметке; повторные mark не создают нового объекта ключа
        metric.mark(actorKey, 5L);
        CheckPoint actorCheckpoint = getCheckpoint(metric, actorKey);
        assertNotNull(actorCheckpoint);
        for (int i = 0; i < 100; i++) {
            metric.mark(actorKey, 5L);
        }
        assertSame("повторные mark не создают новый объект ключа",
                actorCheckpoint, getCheckpoint(metric, actorKey));

        metric.mark(idleKey, 7L);
        CheckPoint idleCheckpoint = getCheckpoint(metric, idleKey);
        assertNotNull(idleCheckpoint);
        metric.mark(idleKey, 7L);
        assertSame(idleCheckpoint, getCheckpoint(metric, idleKey));

        // 2. Первый дамп: оба ключа активны (101 и 2 отметки)
        RecordingListener first = new RecordingListener();
        metric.dump(first);
        assertEquals(2, first.datasets.size());
        assertEquals(101L, first.counts.get(actorKey).longValue());
        assertEquals(5.0, first.means.get(actorKey), 0.0);
        assertEquals(2L, first.counts.get(idleKey).longValue());

        // 3. Второй дамп: отмечен только первый ключ, второй — idle
        metric.mark(actorKey, 5L);
        RecordingListener second = new RecordingListener();
        metric.dump(second);

        // dump обходит ВСЕ ключи
        assertEquals(2, second.datasets.size());
        assertTrue(second.datasets.contains(actorKey));
        assertTrue(second.datasets.contains(idleKey));
        assertEquals(1L, second.counts.get(actorKey).longValue());

        // idle-ключ (без активности с прошлого дампа): дамп отдаёт count=0, mean=NaN.
        assertEquals(0L, second.counts.get(idleKey).longValue());
        assertTrue(Double.isNaN(second.means.get(idleKey)));
    }
}
