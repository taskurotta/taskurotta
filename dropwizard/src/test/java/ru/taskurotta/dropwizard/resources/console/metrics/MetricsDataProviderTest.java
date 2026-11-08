package ru.taskurotta.dropwizard.resources.console.metrics;

import org.junit.Test;
import ru.taskurotta.service.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.service.console.retriever.metrics.MetricsNumberDataRetriever;
import ru.taskurotta.service.metrics.MetricsDataUtils;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Формат ответа, подмножество «period/шаг» точек с хвоста кольца, метки;
 * label = имя датасета. Ручные стабы ретриверов — без синглтонов и spring.
 */
public class MetricsDataProviderTest {

    // Фиксированные константы времени — детерминизм
    private static final long T0 = 1_000_000L;
    private static final long DT = 1000L;

    private static final int RAW_POINTS = 100;

    private static DataPointVO<Long> longPoint(long value, long time) {
        return new DataPointVO<Long>(value, time);
    }

    private static DataPointVO<Double> doublePoint(double value, long time) {
        return new DataPointVO<Double>(value, time);
    }

    private static void assertSameRow(List<Number[]> expected, List<Number[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Number[] exp = expected.get(i);
            Number[] act = actual.get(i);
            if (exp[0] == null) {
                assertEquals("time[" + i + "]", exp[0], act[0]);
            } else {
                assertEquals("time[" + i + "]", exp[0].longValue(), act[0].longValue());
            }
            if (exp[1] == null) {
                assertEquals("value[" + i + "]", exp[1], act[1]);
            } else {
                assertEquals("value[" + i + "]", exp[1].doubleValue(), act[1].doubleValue(), 0.0);
            }
        }
    }

    /** Ручной стаб: фиксированные ряды + запись вызванного метода. Каждый вызов — новый массив. */
    private static final class StubMethodRetriever implements MetricsMethodDataRetriever {
        final List<String> metricNames;
        final DataPointVO<Long>[] countsForLastHour;
        final DataPointVO<Long>[] countsForLastDay;
        final DataPointVO<Double>[] meansForLastHour;
        final DataPointVO<Double>[] meansForLastDay;
        String lastCall;

        StubMethodRetriever(List<String> metricNames,
                            DataPointVO<Long>[] countsForLastHour, DataPointVO<Long>[] countsForLastDay,
                            DataPointVO<Double>[] meansForLastHour, DataPointVO<Double>[] meansForLastDay) {
            this.metricNames = metricNames;
            this.countsForLastHour = countsForLastHour;
            this.countsForLastDay = countsForLastDay;
            this.meansForLastHour = meansForLastHour;
            this.meansForLastDay = meansForLastDay;
        }

        static StubMethodRetriever empty() {
            return new StubMethodRetriever(Collections.<String>emptyList(),
                    new DataPointVO[0], new DataPointVO[0], new DataPointVO[0], new DataPointVO[0]);
        }

        @Override
        public Collection<String> getMetricNames() {
            return metricNames;
        }

        @Override
        public Collection<String> getDataSetsNames(String metricName) {
            return Collections.emptyList();
        }

        @Override
        public long getTotalCountOfLastHour(String metricName, String datasetName) {
            return 0;
        }

        @Override
        public long getTotalCountOfLastDay(String metricName, String datasetName) {
            return 0;
        }

        @Override
        public DataPointVO<Long>[] getCountsForLastHour(String metricName, String datasetName) {
            lastCall = "countsForLastHour";
            return clone(countsForLastHour);
        }

        @Override
        public DataPointVO<Long>[] getCountsForLastDay(String metricName, String datasetName) {
            lastCall = "countsForLastDay";
            return clone(countsForLastDay);
        }

        @Override
        public DataPointVO<Double>[] getMeansForLastHour(String metricName, String datasetName) {
            lastCall = "meansForLastHour";
            return clone(meansForLastHour);
        }

        @Override
        public DataPointVO<Double>[] getMeansForLastDay(String metricName, String datasetName) {
            lastCall = "meansForLastDay";
            return clone(meansForLastDay);
        }

        @Override
        public Date getLastActivityTime(String metricName, String datasetName) {
            return null;
        }

        @Override
        public long getLastActivityTimeMillis(String metricName, String datasetName) {
            return 0;
        }

        @Override
        public ru.taskurotta.service.metrics.handler.DatasetSummary getDatasetSummary(
                String metricName, String datasetName) {
            return null;
        }

        @SuppressWarnings("unchecked")
        private static <T extends Number> DataPointVO<T>[] clone(DataPointVO<T>[] source) {
            return Arrays.copyOf(source, source.length);
        }
    }

    /** Ручной стаб: фиксированный ряд из RAW_POINTS точек (значение = индекс, время = T0+i·DT). */
    private static final class StubNumberRetriever implements MetricsNumberDataRetriever {
        final List<String> metricNames;
        final boolean provideData;

        StubNumberRetriever(List<String> metricNames, boolean provideData) {
            this.metricNames = metricNames;
            this.provideData = provideData;
        }

        static StubNumberRetriever withData() {
            return new StubNumberRetriever(Arrays.asList("queueSize", "memory"), true);
        }

        static StubNumberRetriever empty() {
            return new StubNumberRetriever(Collections.<String>emptyList(), false);
        }

        @Override
        public Collection<String> getNumberMetricNames() {
            return metricNames;
        }

        @Override
        public Collection<String> getNumberDataSets(String metricName) {
            return Collections.emptyList();
        }

        @Override
        public DataPointVO<Number>[] getData(String metricName, String datasetName) {
            if (!provideData) {
                return new DataPointVO[0];
            }
            DataPointVO<Number>[] data = new DataPointVO[RAW_POINTS];
            for (int i = 0; i < RAW_POINTS; i++) {
                data[i] = new DataPointVO<Number>(i, T0 + i * DT);
            }
            return data;
        }

        @Override
        public Date getLastActivityTime(String metricName, String datasetName) {
            return null;
        }

        @Override
        public Number getLastValue(String metricName, String datasetName) {
            return null;
        }
    }

    private static MetricsDataProvider newProvider(MetricsMethodDataRetriever method,
                                                   MetricsNumberDataRetriever number,
                                                   int periodicBinSeconds) {
        MetricsDataProvider provider = new MetricsDataProvider();
        provider.setMethodDataRetriever(method);
        provider.setNumberDataRetriever(number);
        provider.setMethodMetricsPeriodSeconds(1);
        provider.setPeriodicBinSeconds(periodicBinSeconds);
        return provider;
    }

    /**
     * Числовая метрика — только type=size; подмножество = последние
     * period/шаг точек кольца; метки yFormatter="memory", yLabel="Memory",
     * xFormatter="time", xLabel="Time"; label = имя датасета.
     *
     * Шаг подмножества — агрегационный интервал бина (periodicBinSeconds,
     * единое свойство), НЕ период выборки (карта periodicMetricPeriods удалена):
     * кольцо числовых метрик — 24-ч окно 1-мин бинами, подмножество считается от него.
     */
    @Test
    public void numberDataset_SizeOnly_SubsetAndLabels() {
        MetricsDataProvider provider = newProvider(StubMethodRetriever.empty(), StubNumberRetriever.withData(), 10);

        // бин 10 с, период 5 минут → 60*5/10 = 30 точек с хвоста (оба датасета)
        List<ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO> response =
                provider.getDataResponse("queueSize", Arrays.asList("someQueue"), "local", "size", "5minutes");
        assertEquals(1, response.size());
        ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO ds = response.get(0);
        assertEquals("someQueue", ds.getLabel());
        assertEquals("memory", ds.getyFormatter());
        assertEquals("Memory", ds.getyLabel());
        assertEquals("time", ds.getxFormatter());
        assertEquals("Time", ds.getxLabel());
        assertEquals(30, ds.getData().size());
        assertEquals(T0 + 70 * DT, ds.getData().get(0)[0].longValue());
        assertEquals(70.0, ds.getData().get(0)[1].doubleValue(), 0.0);
        assertEquals(T0 + 99 * DT, ds.getData().get(ds.getData().size() - 1)[0].longValue());
        assertEquals(99.0, ds.getData().get(ds.getData().size() - 1)[1].doubleValue(), 0.0);

        // memory: тот же bin-шаг → 300/10 = 30 точек с хвоста
        response = provider.getDataResponse("memory", Arrays.asList("memory"), "local", "size", "5minutes");
        ds = response.get(0);
        assertEquals("memory", ds.getLabel());
        assertEquals(30, ds.getData().size());
        assertEquals(T0 + 70 * DT, ds.getData().get(0)[0].longValue());
        assertEquals(70.0, ds.getData().get(0)[1].doubleValue(), 0.0);

        // type != size для числовой метрики → IllegalArgumentException
        try {
            provider.getDataResponse("queueSize", Arrays.asList("someQueue"), "local", "rate", "1hour");
            fail("ожидался IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Любая другая комбинация → IAE (→ 500 на уровне ресурса)
        }
    }

    /**
     * Методическая метрика — ветки rate/mean × 1hour/24hours
     * вызывают соответствующий ретривер, данные проходят в ответ как есть;
     * прочие комбинации → IAE; метки xFormatter="time", xLabel="Time",
     * yLabel: rate → "Measured, times", mean → "Mean time, ms"; label = имя датасета.
     */
    @Test
    public void methodDataset_RateMean_HourDay() {
        DataPointVO<Long>[] hourCounts = new DataPointVO[]{
                longPoint(1L, 1000L), longPoint(2L, 2000L), longPoint(3L, 3000L)
        };
        DataPointVO<Long>[] dayCounts = new DataPointVO[]{
                longPoint(7L, 50000L)
        };
        DataPointVO<Double>[] hourMeans = new DataPointVO[]{
                doublePoint(10.0, 100L), doublePoint(20.0, 200L)
        };
        DataPointVO<Double>[] dayMeans = new DataPointVO[]{
                doublePoint(30.0, 90000L)
        };
        StubMethodRetriever retriever = new StubMethodRetriever(
                Arrays.asList("poll", "startProcess"), hourCounts, dayCounts, hourMeans, dayMeans);
        MetricsDataProvider provider = newProvider(retriever, StubNumberRetriever.empty(), 10);

        // rate × 1hour → counts за час
        ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO ds =
                provider.getDataResponse("poll", Arrays.asList("actorA"), "local", "rate", "1hour").get(0);
        assertEquals("actorA", ds.getLabel());
        assertEquals("time", ds.getxFormatter());
        assertEquals("Time", ds.getxLabel());
        assertEquals("Measured, times", ds.getyLabel());
        assertEquals("countsForLastHour", retriever.lastCall);
        assertSameRow(MetricsDataUtils.convertToTimedDataRow(Arrays.copyOf(hourCounts, hourCounts.length)), ds.getData());

        // rate × 24hours → counts за сутки
        ds = provider.getDataResponse("poll", Arrays.asList("actorA"), "local", "rate", "24hours").get(0);
        assertEquals("Measured, times", ds.getyLabel());
        assertEquals("countsForLastDay", retriever.lastCall);
        assertSameRow(MetricsDataUtils.convertToTimedDataRow(Arrays.copyOf(dayCounts, dayCounts.length)), ds.getData());

        // mean × 1hour → means за час
        ds = provider.getDataResponse("poll", Arrays.asList("actorA"), "local", "mean", "1hour").get(0);
        assertEquals("Mean time, ms", ds.getyLabel());
        assertEquals("meansForLastHour", retriever.lastCall);
        assertSameRow(MetricsDataUtils.convertToTimedDataRow(Arrays.copyOf(hourMeans, hourMeans.length)), ds.getData());

        // mean × 24hours → means за сутки
        ds = provider.getDataResponse("poll", Arrays.asList("actorA"), "local", "mean", "24hours").get(0);
        assertEquals("Mean time, ms", ds.getyLabel());
        assertEquals("meansForLastDay", retriever.lastCall);
        assertSameRow(MetricsDataUtils.convertToTimedDataRow(Arrays.copyOf(dayMeans, dayMeans.length)), ds.getData());

        // прочие комбинации → IllegalArgumentException
        try {
            provider.getDataResponse("poll", Arrays.asList("actorA"), "local", "size", "1hour");
            fail("ожидался IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            provider.getDataResponse("poll", Arrays.asList("actorA"), "local", "mean", "5minutes");
            fail("ожидался IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }
}
