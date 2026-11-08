package ru.taskurotta.service.metrics;

import org.junit.Test;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Чистые функции конвейера ответа консоли: точные суммы и среднее;
 * [time, value] + ровно один маркер [null, null] на прогон null;
 * сортировка по времени, null первыми.
 */
public class MetricsDataUtilsTest {

    // Фиксированные константы времени — детерминизм
    private static final long T0 = 1_000_000L;
    private static final long DT = 1000L;

    private static long timeAt(int i) {
        return T0 + i * DT;
    }

    private static DataPointVO<Long> point(long value, long time) {
        return new DataPointVO<Long>(value, time);
    }

    private static DataPointVO<Double> pointD(double value, long time) {
        return new DataPointVO<Double>(value, time);
    }

    @Test
    public void totalsMeans_Smoothing_NonZero() {
        // ===== getTotalCounts — точная сумма заполненных не-отрицательных слотов =====
        DataPointVO<Long>[] counts = new DataPointVO[]{
                point(5L, timeAt(0)), null, point(0L, timeAt(2)), point(7L, timeAt(3))
        };
        assertEquals(12L, MetricsDataUtils.getTotalCounts(counts));

        // ===== getMeanTime — среднее по записанным не-отрицательным значениям =====
        DataPointVO<Double>[] means = new DataPointVO[]{
                pointD(10.0, timeAt(0)), pointD(20.0, timeAt(1)), null, pointD(-5.0, timeAt(3))
        };
        // null и отрицательные слоты пропускаются: (10 + 20) / 2
        assertEquals(15.0, MetricsDataUtils.getMeanTime(means), 0.0);

        // ===== sumUpDataPointsArray — точная сумма =====
        DataPointVO<Long>[] sumSource = new DataPointVO[]{
                point(5L, timeAt(0)), point(0L, timeAt(1)), point(7L, timeAt(2)), null
        };
        assertEquals(12L, MetricsDataUtils.sumUpDataPointsArray(sumSource));

        // ===== getSmoothedDataSet — сглаживание: шаг 6 → N/6 точек =====
        List<Number[]> raw = Arrays.asList(
                new Number[]{timeAt(0), 1L}, new Number[]{timeAt(1), 2L}, new Number[]{timeAt(2), 3L},
                new Number[]{timeAt(3), 4L}, new Number[]{timeAt(4), 5L}, new Number[]{timeAt(5), 6L},
                new Number[]{timeAt(6), 7L}, new Number[]{timeAt(7), 8L}, new Number[]{timeAt(8), 9L},
                new Number[]{timeAt(9), 10L}, new Number[]{timeAt(10), 11L}, new Number[]{timeAt(11), 12L}
        );
        List<Number[]> smoothed = MetricsDataUtils.getSmoothedDataSet(raw, 6);
        // 12 точек / шаг 6 = 2 точки: время шестого и двенадцатого, среднее по окну
        // «соседей» i±(times-1), обрезаемому границами массива
        assertEquals(2, smoothed.size());
        assertEquals(timeAt(5), smoothed.get(0)[0].longValue());
        // окно [0,10) — радиус 5 слева/справа от i=5, обрезанное стартом: (1+..+10)/10
        assertEquals(5L, smoothed.get(0)[1].longValue());
        assertEquals(timeAt(11), smoothed.get(1)[0].longValue());
        // окно [6,12) — обрезано концом: (7+8+9+10+11+12)/6
        assertEquals(9L, smoothed.get(1)[1].longValue());

        // ===== getNonZeroValuesDataSet — фильтрация: удаляются [null, null], [null, v], отрицательные;
        // ===== точки с не-отрицательным значением (включая 0) остаются =====
        List<Number[]> withGaps = Arrays.asList(
                new Number[]{timeAt(0), 5L},
                new Number[]{timeAt(1), 0L},
                new Number[]{null, null},
                new Number[]{timeAt(3), -1L},
                new Number[]{null, 3L},
                new Number[]{timeAt(5), 7L}
        );
        List<Number[]> filtered = MetricsDataUtils.getNonZeroValuesDataSet(withGaps);
        assertEquals(3, filtered.size());
        assertEquals(5L, filtered.get(0)[1].longValue());
        assertEquals(0L, filtered.get(1)[1].longValue());
        assertEquals(7L, filtered.get(2)[1].longValue());

        // ===== sortDataSet — по возрастанию времени, null первыми =====
        DataPointVO<Long>[] unsorted = new DataPointVO[]{
                point(3L, timeAt(2)), null, point(1L, timeAt(0)), point(2L, timeAt(1))
        };
        MetricsDataUtils.sortDataSet(unsorted);
        assertNull(unsorted[0]);
        assertEquals(timeAt(0), unsorted[1].getTime());
        assertEquals(timeAt(1), unsorted[2].getTime());
        assertEquals(timeAt(2), unsorted[3].getTime());

        // ===== convertToTimedDataRow — пары [time, value] + ровно ОДИН маркер [null, null] на прогон null =====
        DataPointVO<Long>[] withNullGap = new DataPointVO[]{
                point(5L, timeAt(0)), null, null, point(7L, timeAt(3))
        };
        List<Number[]> timedRow = MetricsDataUtils.convertToTimedDataRow(withNullGap);
        // прогон из ДВУХ null → один маркер
        assertEquals(3, timedRow.size());
        assertEquals(timeAt(0), timedRow.get(0)[0].longValue());
        assertEquals(5L, timedRow.get(0)[1].longValue());
        assertNull(timedRow.get(1)[0]);
        assertNull(timedRow.get(1)[1]);
        assertEquals(timeAt(3), timedRow.get(2)[0].longValue());
        assertEquals(7L, timedRow.get(2)[1].longValue());
    }
}
