package ru.taskurotta.service.metrics;

import org.junit.Test;
import ru.taskurotta.service.metrics.handler.DatasetSummary;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.model.DataPointVO;
import ru.taskurotta.service.metrics.model.DataRowVO;
import ru.taskurotta.service.metrics.model.NumberDataRowVO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Ядро биннинга: время-базовые бины, накопление count,
 * взвешенный merge mean, wrap по окну, отдача только полных бин, idle без точек,
 * getLastValue = свежайший заполненный бин.
 *
 * Детерминизм: фиксированные measurementTime, без wall-clock
 * ассертов (геттеры полных бин принимают `now` явно).
 */
public class MetricsBinningTest {

    // Фиксированное время в далёком прошлом, на границе бина:
    // кратно и 10 с, и 60 с (99996 * 10000 = 999960000; 16666 * 60000 = 999960000)
    private static final long T0 = 999_960_000L;
    private static final long BIN_10S = 10_000L;
    private static final long BIN_60S = 60_000L;

    /** То же выражение, что и в BaseDataRowVO: (time / binMillis) % size. */
    private static int binIndex(long time, long binMillis, int size) {
        return (int) ((time / binMillis) % size);
    }

    /**
     * Два тика в одном бине: count суммируется, mean — среднее, взвешенное по count.
     * mean' = (mean_old * count_old + mean_new * count_new) / (count_old + count_new)
     *      = (100 * 10 + 200 * 30) / 40 = 175.
     */
    @Test
    public void twoTicksSameBin_CountSummed_MeanWeighted() {
        DataRowVO row = new DataRowVO(36, BIN_10S, "metric", "dataset");

        row.populate(10L, 100.0, T0);
        row.populate(30L, 200.0, T0 + 5_000L);//тот же 10-с бин

        DataPointVO<Long> countPoint = row.getDsCounts().get(binIndex(T0, BIN_10S, 36));
        DataPointVO<Double> meanPoint = row.getDsMean().get(binIndex(T0, BIN_10S, 36));
        assertEquals(Long.valueOf(40L), countPoint.getValue());
        assertEquals(175.0, meanPoint.getValue(), 0.0);

        // Сумма кольца = накопленный count
        assertEquals(40L, row.getTotalCount());
    }

    /**
     * Wrap: t и t + окно (360 бин * 10 с = 3600 с) — один и тот же бин;
     * данные прежнего оборота сбрасываются (кольцо — скользящее окно),
     * время бина — время последней отметки.
     */
    @Test
    public void wrap_TimePlusWindow_SameBin() {
        DataRowVO row = new DataRowVO(360, BIN_10S, "metric", "dataset");

        row.populate(1L, 0.0, T0);
        row.populate(2L, 0.0, T0 + 3_600_000L);//t + 1 час — слот переиспользован

        DataPointVO<Long> countPoint = row.getDsCounts().get(binIndex(T0, BIN_10S, 360));
        assertEquals(Long.valueOf(2L), countPoint.getValue());
        assertEquals(T0 + 3_600_000L, countPoint.getTime());
        assertEquals(2L, row.getTotalCount());
    }

    /**
     * Недополненный текущий бин (binStart + binSize > now): в отдаче точек —
     * нет (только полные бины), в сумме «всего за окно» — учитывается.
     */
    @Test
    public void incompleteBin_NotInPoints_ButInTotal() {
        // Кольцо 4 бина * 10 с; now внутри третьего бина
        DataRowVO row = new DataRowVO(4, BIN_10S, "metric", "dataset");
        long now = T0 + 35_000L;//бин [T0+30s, T0+40s) — в работе
        row.populate(7L, 0.0, T0);        //бин 0 — полный
        row.populate(3L, 0.0, T0 + 30_000L);//бин 3 — недополненный (текущий)

        DataPointVO<Long>[] complete = row.getCompleteCounts(now);
        assertEquals(3, complete.length);
        boolean currentBinPresent = false;
        boolean firstBinPresent = false;
        for (DataPointVO<Long> point : complete) {
            if (point != null && point.getTime() == T0 + 30_000L) {
                currentBinPresent = true;
            }
            if (point != null && point.getTime() == T0) {
                firstBinPresent = true;
            }
        }
        assertTrue("недополненный бин не должен быть в отдаче", !currentBinPresent);
        assertTrue("полный бин должен быть в отдаче", firstBinPresent);

        // Сумма включает и недополненный бин
        assertEquals(10L, row.getTotalCount());
    }

    /**
     * Idle-дамп (count=0): строку не создаёт, точку не пишет, lastActive не двигает.
     */
    @Test
    public void idle_CreatesNoRow_DoesNotMoveLastActive() {
        MetricsDataHandler handler = new MetricsDataHandler(10, 120, 1500, 86400, 600);

        handler.handle("metric", "dataset", 0L, Double.NaN, T0);
        assertNull("idle-дамп строку не создаёт", handler.getCountsForLastHour("metric", "dataset"));
        assertNull("idle-дамп суточную строку не создаёт", handler.getCountsForLastDay("metric", "dataset"));

        handler.handle("metric", "dataset", 5L, 1.0, T0 + 10_000L);
        assertEquals(T0 + 10_000L, handler.getLastActivityTimeMillis("metric", "dataset"));

        // idle после активности — lastActive не двигает
        handler.handle("metric", "dataset", 0L, Double.NaN, T0 + 20_000L);
        assertEquals(T0 + 10_000L, handler.getLastActivityTimeMillis("metric", "dataset"));
    }

    /**
     * getLastValue (NumberDataRowVO): значение бина, в который попала последняя запись;
     * запись в более ранний бин после свежей («старая» точка) не сбрасывает результат.
     */
    @Test
    public void getLastValue_NewestFilledBin() {
        NumberDataRowVO row = new NumberDataRowVO(24, BIN_60S, "queueSize", "queue");

        assertNull("пустой ряд — null (null-guard фичи 002)", row.getLastValue());

        row.populate(Integer.valueOf(1), T0);
        assertEquals(Integer.valueOf(1), row.getLastValue());

        // свежая выборка в следующем 1-мин бине
        row.populate(Integer.valueOf(2), T0 + 70_000L);
        assertEquals(Integer.valueOf(2), row.getLastValue());

        // «старая» точка в бине T0: последняя активность осталась T0+70s → 2
        row.populate(Integer.valueOf(3), T0);
        assertEquals(Integer.valueOf(2), row.getLastValue());
    }

    /**
     * ПУТЬ СУММЫ ЧЕРЕЗ ОБОРОТ на уровне handler (не только класса):
     * реальный MetricsDataHandler (без init(), как в idle-тесте), данные проходят
     * полный оборот кольца — «всего за окно» отдаёт только текущее окно, без
     * накопления предыдущих оборотов.
     * handle() пишет и в часовое, и в суточное кольцо — пары metric/dataset разные.
     */
    @Test
    public void handler_AfterFullWrap_TotalsAreOnlyCurrentWindow() {
        MetricsDataHandler handler = new MetricsDataHandler(10, 120, 1500, 86400, 600);

        // Часовое кольцо (360 бин × 10 с): 2 полных оборота — 360 + 360 handle по 10 с
        for (int i = 0; i < 360; i++) {
            handler.handle("poll", "poll", 1L, 0.0, T0 + i * BIN_10S);                 // оборот 1
        }
        for (int i = 0; i < 360; i++) {
            handler.handle("poll", "poll", 1L, 0.0, T0 + (i + 360) * BIN_10S);         // оборот 2: +3600 с
        }
        assertEquals("«всего за час» после полного оборота — только текущее окно (360), не 720",
                360L, handler.getTotalCountOfLastHour("poll", "poll"));
        DatasetSummary pollSummary = handler.getDatasetSummary("poll", "poll");
        assertEquals("сводка часа — только текущее окно", 360L, pollSummary.getHour().getCount());

        // Суточное кольцо (720 бин × 120 с): 2 полных оборота — 720 + 720 handle по 120 с
        for (int i = 0; i < 720; i++) {
            handler.handle("enqueue", "enqueue", 1L, 0.0, T0 + i * 120_000L);          // оборот 1
        }
        for (int i = 0; i < 720; i++) {
            handler.handle("enqueue", "enqueue", 1L, 0.0, T0 + (i + 720) * 120_000L);  // оборот 2: +86400 с
        }
        assertEquals("«всего за сутки» после полного оборота — только текущее окно (720), не 1440",
                720L, handler.getTotalCountOfLastDay("enqueue", "enqueue"));
        DatasetSummary enqueueSummary = handler.getDatasetSummary("enqueue", "enqueue");
        assertEquals("сводка суток — только текущее окно", 720L, enqueueSummary.getDay().getCount());

        // Часовое кольцо для суточной пары: данные на 48 часов = 48 оборотов часового кольца.
        // Последнее окно (3600 с) содержит 3600/120 = 30 отметок — по одной в каждом занятом бине.
        assertEquals("48 оборотов часового кольца без накопления: 30, не 30×48",
                30L, handler.getTotalCountOfLastHour("enqueue", "enqueue"));
    }

    /**
     * Last-write-wins внутри бина: в бине остаётся последняя выборка.
     */
    @Test
    public void numberBin_LastWriteWins() {
        NumberDataRowVO row = new NumberDataRowVO(24, BIN_60S, "queueSize", "queue");

        row.populate(Integer.valueOf(10), T0);
        row.populate(Integer.valueOf(20), T0 + 30_000L);//тот же 1-мин бин
        row.populate(Integer.valueOf(30), T0 + 60_000L);//следующий бин

        DataPointVO<Number>[] complete = row.getCompleteData(T0 + 200_000L);
        DataPointVO<Number> binPoint = null;
        for (DataPointVO<Number> point : complete) {
            if (point != null && point.getTime() == T0 + 30_000L) {
                binPoint = point;
            }
        }
        assertNotNull("точка бина T0 должна быть в полных бинах", binPoint);
        assertEquals("в бине T0 осталась последняя выборка (last-write-wins)",
                Integer.valueOf(20), binPoint.getValue());
        assertEquals(Integer.valueOf(30), row.getLastValue());
    }
}
