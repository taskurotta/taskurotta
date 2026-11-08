package ru.taskurotta.dropwizard.resources.console.metrics;

import org.junit.Test;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO;
import ru.taskurotta.service.console.retriever.metrics.MetricsNumberDataRetriever;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * MetricsDataProvider на данных, прошедших полный оборот кольца.
 * Читающий путь — реальный: provider + реальный MetricsDataHandler (он сам
 * implements MetricsMethodDataRetriever) без init() (как в MetricsBinningTest).
 * Данные постампованы так, что обрываются ≈ текущему wall-clock моменту:
 * «окно последнего часа» = последние 3600 с поинъектированных данных,
 * исключается ровно один бин binIndex(now) (допуск — 1 бин).
 *
 * Ассерты: размер подмножества (полные бины = size−1), ВСЕ времена точек —
 * в окне последнего оборота, сумма = сумма поинъектированного за окно,
 * числовой путь — подмножество period/шаг от данных реального кольца.
 */
public class MetricsDataProviderWrapTest {

    private static final long BIN_10S = 10_000L;

    private static long sumNonNull(List<Number[]> points) {
        long sum = 0;
        for (Number[] p : points) {
            if (p[0] != null && p[1] != null) {
                sum += p[1].longValue();
            }
        }
        return sum;
    }

    private static int nonNullCount(List<Number[]> points) {
        int count = 0;
        for (Number[] p : points) {
            if (p[0] != null && p[1] != null) {
                count++;
            }
        }
        return count;
    }

    /** Минимальный пустой стаб числового ретривера (числовой путь — отдельный тест). */
    private static final class EmptyNumberRetriever implements MetricsNumberDataRetriever {

        @Override
        public Collection<String> getNumberMetricNames() {
            return Collections.emptySet();
        }

        @Override
        public Collection<String> getNumberDataSets(String metricName) {
            return Collections.emptySet();
        }

        @Override
        public DataPointVO<Number>[] getData(String metricName, String datasetName) {
            return null;
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

    private static MetricsDataProvider newProvider(MetricsDataHandler method, MetricsNumberDataRetriever number) {
        MetricsDataProvider provider = new MetricsDataProvider();
        provider.setMethodDataRetriever(method);
        provider.setNumberDataRetriever(number);
        provider.setMethodMetricsPeriodSeconds(1);
        provider.setPeriodicBinSeconds(10);
        return provider;
    }

    /**
     * Методический путь после 2 полных оборотов часового кольца (360 бин × 10 с):
     * отдача = полные бины (359 точек), ВСЕ времена — второго оборота,
     * сумма = 359 × count; «всего за окно» (сумма кольца) = 360 = поинъектированному
     * за последний час (допуск — один недополненный бин).
     */
    @Test
    public void methodPath_AfterFullWrap_PointsOfCurrentWindowOnly() {
        MetricsDataHandler handler = new MetricsDataHandler(10, 120, 1500, 86400, 600);

        long base = (System.currentTimeMillis() / BIN_10S) * BIN_10S;//граница бина
        long t0 = base - 7_200_000L;//2 часа до границы: ровно 2 оборота часового кольца
        for (int i = 0; i < 720; i++) {
            handler.handle("poll", "poll", 1L, 0.0, t0 + i * BIN_10S);
        }

        DatasetVO ds = newProvider(handler, new EmptyNumberRetriever())
                .getDataResponse("poll", Arrays.asList("poll"), "local", "rate", "1hour").get(0);
        List<Number[]> points = ds.getData();

        assertEquals("отдача — полные бины: 360−1 (исключён бин binIndex(now))",
                359, nonNullCount(points));
        assertEquals("все значения — второго оборота (по 1 на бин)", 359L, sumNonNull(points));

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Number[] p : points) {
            if (p[0] != null) {
                min = Math.min(min, p[0].longValue());
                max = Math.max(max, p[0].longValue());
            }
        }
        // Исключённый бин — wall-clock now: binIndex(base) или binIndex(base+10с);
        // в данных второго оборота они несут времена t0+3600с или t0+3610с
        assertTrue("все времена точек — в окне второго оборота (min=" + min + ")",
                min >= t0 + 3_600_000L && min <= t0 + 3_610_000L);
        assertEquals("последняя точка — последний бин второго оборота", t0 + 7_190_000L, max);

        // «Всего за последний час» с допуском (1 бин): сумма кольца = поинъектированному
        assertEquals(360L, handler.getTotalCountOfLastHour("poll", "poll"));
    }

    /**
     * Числовой путь: provider + реальный NumberDataHandler — подмножество =
     * последние period/шаг точек реального кольца (шаг — бин, не период выборки);
     * свежие 20 точек в хвосте кольца полностью в подмножестве.
     */
    @Test
    public void numberPath_RealRing_TailSubsetOfPeriodOverBin() {
        NumberDataHandler number = new NumberDataHandler(10, 1500, 86400, 600);//бин 10 с, кольцо 8640
        long base = (System.currentTimeMillis() / BIN_10S) * BIN_10S;
        for (int i = 0; i < 20; i++) {
            number.handleNumberData("queueSize", "q", Integer.valueOf(i),
                    base - 200_000L + i * BIN_10S, 8640);
        }

        DatasetVO ds = newProvider(new MetricsDataHandler(10, 120, 1500, 86400, 600), number)
                .getDataResponse("queueSize", Arrays.asList("q"), "local", "size", "1hour").get(0);
        List<Number[]> points = ds.getData();

        assertEquals("подмножество period/шаг (3600/10) — в пределах; все 20 свежих точек на месте",
                20, nonNullCount(points));
        assertEquals("первая точка — хвост кольца (0-я выборка)", base - 200_000L, points.get(1)[0].longValue());
        assertEquals("последнее значение — свежайшая выборка", 19, points.get(points.size() - 1)[1].intValue());
        assertTrue("подмножество не больше period/шаг точек", points.size() <= 3600);
    }
}
