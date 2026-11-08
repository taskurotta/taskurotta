package ru.taskurotta.service.metrics;

import org.junit.Test;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Размер кольца периодической метрики = 86400/bin-секунд, НЕ 86400/период выборки;
 * ровно одна точка на занятый 1-мин бин, значение — последняя выборка (last-write-wins).
 * Боевое кольцо — 1440 бин по 1 минуте независимо от периода выборки.
 * Детерминизм: фиксированные времена; latch с таймаутом, без sleep «на глаз».
 */
public class PeriodicMetricTest {

    // Период выборки — ПАРАМЕТР (1 с); бин — 60 с (дефолт фикса памяти)
    private static final int PERIOD_SECONDS = 1;
    private static final int BIN_SECONDS = 60;
    private static final int CYCLES_TO_OBSERVE = 2;

    private static final String METRIC_NAME = "queueSize";
    private static final String DATASET_A = "queue-a";
    private static final String DATASET_B = "queue-b";

    private static final int VALUE_A = 3;
    private static final int VALUE_B = 7;

    // Фиксированное время на границе 1-мин бина (16666 * 60000)
    private static final long T0 = 999_960_000L;

    /** Стаб-экстрактор: фиксированные значения, general = сумма (как у queueSize). */
    private static final class StubExtractor implements PeriodicMetric.DatasetValueExtractor {
        @Override
        public List<String> getDatasets() {
            return Arrays.asList(DATASET_A, DATASET_B);
        }

        @Override
        public Number getDatasetValue(String dataset) {
            if (DATASET_A.equals(dataset)) {
                return Integer.valueOf(VALUE_A);
            }
            if (DATASET_B.equals(dataset)) {
                return Integer.valueOf(VALUE_B);
            }
            throw new IllegalStateException("unexpected dataset: " + dataset);
        }

        @Override
        public Number getGeneralValue(Map<String, Number> datasetsValues) {
            int sum = 0;
            for (Number value : datasetsValues.values()) {
                sum += value.intValue();
            }
            return Integer.valueOf(sum);
        }
    }

    /** Стаб-слушатель: записи в synchronized-список, latch на полное число ожидаемых вызовов. */
    private static final class RecordingNumberListener implements ru.taskurotta.service.metrics.handler.NumberDataListener {
        final List<String[]> calls = new ArrayList<String[]>();
        final CountDownLatch latch;

        RecordingNumberListener(int expectedCalls) {
            this.latch = new CountDownLatch(expectedCalls);
        }

        @Override
        public synchronized void handleNumberData(String metricName, String datasetName, Number value,
                                                  long currentTime, int maxPoints) {
            calls.add(new String[]{metricName, datasetName, String.valueOf(value), String.valueOf(maxPoints)});
            latch.countDown();
        }
    }

    /**
     * Размер кольца периодической метрики = 86400/bin-секунд
     * (1440) — функция параметра bin, НЕ периода выборки.
     * Плюс несменное: за каждый период — ровно одна точка на каждый датасет + одна
     * general под `metric#metric`.
     */
    @Test
    public void periodicMark_PointsPerDatasetAndGeneral() throws Exception {
        final RecordingNumberListener listener = new RecordingNumberListener(CYCLES_TO_OBSERVE * 3);
        MetricsFactory factory = new MetricsFactory(1, 2, new MetricsDataHandler(), listener, BIN_SECONDS);
        try {
            PeriodicMetric metric = factory.getPeriodicInstance(METRIC_NAME, PERIOD_SECONDS);
            metric.periodicMark(new StubExtractor());

            // 2 цикла ожидания через latch с таймаутом, без Thread.sleep «на глаз»
            boolean waited = listener.latch.await(15, TimeUnit.SECONDS);
            assertTrue("недождались 2 циклов выборки", waited);

            // Снимок: не менее 6 вызовов гарантированно записаны (latch отсчитал их)
            List<String[]> snapshot;
            synchronized (listener) {
                snapshot = new ArrayList<String[]>(listener.calls);
            }

            // Размер кольца = 86400/bin-секунд (1440), не 86400/период (86400)
            final int expectedDataSize = TimeConstants.SECONDS_IN_24_HOURS / BIN_SECONDS;
            assertEquals(1440, expectedDataSize);

            // Каждые 3 вызова — один цикл: ровно одна точка на каждый датасет + одна general
            // под `metric#metric` (датасет = имя метрики, конвенция двойной отметки)
            for (int cycle = 0; cycle < CYCLES_TO_OBSERVE; cycle++) {
                String[] pointA = snapshot.get(cycle * 3);
                String[] pointB = snapshot.get(cycle * 3 + 1);
                String[] general = snapshot.get(cycle * 3 + 2);

                assertEquals(METRIC_NAME, pointA[0]);
                assertEquals(DATASET_A, pointA[1]);
                assertEquals(String.valueOf(VALUE_A), pointA[2]);
                assertEquals(String.valueOf(expectedDataSize), pointA[3]);

                assertEquals(METRIC_NAME, pointB[0]);
                assertEquals(DATASET_B, pointB[1]);
                assertEquals(String.valueOf(VALUE_B), pointB[2]);
                assertEquals(String.valueOf(expectedDataSize), pointB[3]);

                assertEquals(METRIC_NAME, general[0]);
                assertEquals("general-датасет = имя метрики (ключ metric#metric)", METRIC_NAME, general[1]);
                assertEquals(String.valueOf(VALUE_A + VALUE_B), general[2]);
                assertEquals(String.valueOf(expectedDataSize), general[3]);
            }
        } finally {
            factory.shutdown();
        }
    }

    /**
     * Ровно одна точка на занятый 1-мин бин, значение — последняя выборка:
     * три выборки в один бин (период 1 с < бин 60 с) — одна точка, last-write-wins.
     */
    @Test
    public void oneMinuteBin_OnePointLastSample() {
        NumberDataHandler handler = new NumberDataHandler(BIN_SECONDS, 1500, 86400, 600);

        handler.handleNumberData(METRIC_NAME, DATASET_A, Integer.valueOf(1), T0, 1440);
        handler.handleNumberData(METRIC_NAME, DATASET_A, Integer.valueOf(2), T0 + 10_000L, 1440);
        handler.handleNumberData(METRIC_NAME, DATASET_A, Integer.valueOf(3), T0 + 20_000L, 1440);

        DataPointVO<Number>[] data = handler.getData(METRIC_NAME, DATASET_A);
        assertNotNull(data);
        DataPointVO<Number> binPoint = null;
        for (DataPointVO<Number> point : data) {
            if (point != null && point.getTime() <= T0 + 20_000L) {
                assertNull("ожидается ровно одна точка в бине", binPoint);
                binPoint = point;
            }
        }
        assertNotNull("точка бина T0 должна быть в отдаче", binPoint);
        assertEquals("в бине осталась последняя выборка", Integer.valueOf(3), binPoint.getValue());
        assertEquals("getLastValue — свежайший заполненный бин", Integer.valueOf(3), handler.getLastValue(METRIC_NAME, DATASET_A));
    }
}
