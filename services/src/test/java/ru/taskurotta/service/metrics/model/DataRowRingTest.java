package ru.taskurotta.service.metrics.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Время-базовые бины: count накапливается, mean — взвешенный merge по count,
 * idle без точек, отдача — только полные бины, сумма «всего» включает недополненный бин.
 * Детерминизм: фиксированные measurementTime, без wall-clock ассертов.
 */
public class DataRowRingTest {

    // Фиксированное время на границе бина: кратно 10 с (99996 * 10000)
    private static final long T0 = 999_960_000L;
    private static final long BIN = 10_000L;//10-с бин

    private static long timeAt(int callIndex) {
        return T0 + (callIndex - 1L) * BIN;
    }

    private static int binIndex(long time, int size) {
        return (int) ((time / BIN) % size);
    }

    @Test
    public void populate_WrapSumMeanSummary() {
        // ===== 1. Wrap: кольцо 8 бин (окно 80 с); t и t + 8 * bin — один бин =====
        // Размер кольца — параметр конструктора, НЕ константы 360/720
        final int size = 8;
        DataRowVO ring = new DataRowVO(size, BIN, "metric", "dataset");
        for (int i = 1; i <= size + 1; i++) {
            ring.populate(i, 0.0, timeAt(i));
        }

        // 9-й вызов (t = T0 + 80 с) — тот же бин, что и 1-й (wrap по окну):
        // данные прежнего оборота сброшены, count бина = 9 (НЕ 1 + 9),
        // время — последней отметки. Кольцо — скользящее окно, не аккумулятор оборотов
        // (дефект, найденный на E2E 001: «всего за час» после wrap накапливал все обороты)
        assertEquals(Long.valueOf(9L), ring.getDsCounts().get(binIndex(T0, size)).getValue());
        assertEquals(timeAt(9), ring.getDsCounts().get(binIndex(T0, size)).getTime());
        assertEquals(Long.valueOf(2L), ring.getDsCounts().get(binIndex(timeAt(2), size)).getValue());
        assertEquals(Long.valueOf(8L), ring.getDsCounts().get(binIndex(timeAt(8), size)).getValue());

        // ===== 2. Суммы: getTotalCount() = сумма всех бин кольца (включая недополненный) =====
        // бины: 9, 2, 3, 4, 5, 6, 7, 8
        assertEquals(44L, ring.getTotalCount());

        // ===== 2а. Два оборота: «всего» = только текущее окно, не сумма всех оборотов =====
        DataRowVO twoTurns = new DataRowVO(8, BIN, "metric", "dataset");
        for (int i = 1; i <= 8; i++) {
            twoTurns.populate(10L, 100.0, timeAt(i));              // оборот 1
            twoTurns.populate(20L, 200.0, timeAt(i) + 8 * BIN);     // оборот 2, те же слоты
        }
        assertEquals("«всего» — только текущее окно (8 бин × 20)", 160L, twoTurns.getTotalCount());
        for (int i = 1; i <= 8; i++) {
            int pos = binIndex(timeAt(i), 8);
            assertEquals(Long.valueOf(20L), twoTurns.getDsCounts().get(pos).getValue());
            assertEquals("mean прежнего оборота сброшен", 200.0, twoTurns.getDsMean().get(pos).getValue(), 0.0);
        }

        // ===== 3. Взвешенный merge mean внутри бина: (10*100 + 30*200) / 40 = 175 =====
        DataRowVO meanRing = new DataRowVO(10, BIN, "metric", "dataset");
        meanRing.populate(10L, 100.0, T0);
        meanRing.populate(30L, 200.0, T0 + 5_000L);//тот же бин
        DataPointVO<Double> mergedMean = meanRing.getDsMean().get(binIndex(T0, 10));
        assertEquals(175.0, mergedMean.getValue(), 0.0);
        assertEquals(Long.valueOf(40L), meanRing.getDsCounts().get(binIndex(T0, 10)).getValue());

        // ===== 4. getSummary: средневзвешенное по count + count + timeMin/timeMax =====
        DataRowVO summaryRing = new DataRowVO(10, BIN, "metric", "dataset");
        summaryRing.populate(2L, 100.0, T0);
        summaryRing.populate(3L, 200.0, T0 + BIN);//следующий бин
        DataRowSummary summary = summaryRing.getSummary();
        // mean = (100*2 + 200*3) / (2+3) — средневзвешенное по count
        assertEquals(160.0, summary.getMean(), 0.0);
        assertEquals(5L, summary.getCount());
        assertEquals(T0, summary.getTimeMin());
        assertEquals(T0 + BIN, summary.getTimeMax());

        // ===== 5. lastActive методической строки обновляется ТОЛЬКО при count>0 =====
        DataRowVO lastActiveRing = new DataRowVO(8, BIN, "metric", "dataset");
        lastActiveRing.populate(1L, 0.0, T0);
        assertEquals(T0, lastActiveRing.getLatestActivity());
        // idle-счётчик (count=0) «последнюю активность» не двигает, точку не пишет
        assertEquals(-1, lastActiveRing.populate(0L, 0.0, T0 + BIN));
        assertEquals(T0, lastActiveRing.getLatestActivity());
        lastActiveRing.populate(1L, 0.0, T0 + 2 * BIN);
        assertEquals(T0 + 2 * BIN, lastActiveRing.getLatestActivity());

        // ===== 6. Отдача только полных бин; сумма — включая недополненный =====
        // Кольцо 4 бина; now внутри третьего бина — он недополненный
        DataRowVO completeRing = new DataRowVO(4, BIN, "metric", "dataset");
        long now = T0 + 35_000L;
        completeRing.populate(7L, 0.0, T0);//полный бин
        completeRing.populate(3L, 0.0, T0 + 30_000L);//недополненный (текущий) бин
        DataPointVO<Long>[] complete = completeRing.getCompleteCounts(now);
        assertEquals(3, complete.length);
        boolean incompletePresent = false;
        boolean completePresent = false;
        for (DataPointVO<Long> point : complete) {
            if (point != null && point.getTime() == T0 + 30_000L) {
                incompletePresent = true;
            }
            if (point != null && point.getTime() == T0) {
                completePresent = true;
            }
        }
        assertTrue("недополненный бин не в отдаче", !incompletePresent);
        assertTrue("полный бин в отдаче", completePresent);
        assertNull("пустые бины остаются дырами (null)", complete[binIndex(T0 + BIN, 4)]);
        assertEquals("сумма включает недополненный бин", 10L, completeRing.getTotalCount());
    }

    /**
     * getSummary ПОСЛЕ полного оборота — только текущее окно:
     * count не суммируется обороты, mean не смешивается, timeMin/timeMax —
     * времена последнего оборота.
     */
    @Test
    public void summary_AfterFullWrap_OnlyCurrentWindow() {
        final int size = 8;
        DataRowVO ring = new DataRowVO(size, BIN, "metric", "dataset");
        for (int i = 1; i <= size; i++) {
            ring.populate(10L, 100.0, timeAt(i));             // оборот 1
            ring.populate(20L, 200.0, timeAt(i) + size * BIN);// оборот 2, те же слоты
        }

        DataRowSummary summary = ring.getSummary();
        assertEquals("count — только текущее окно (8×20), не сумма оборотов (8×10+8×20)",
                160L, summary.getCount());
        assertEquals("mean — только текущее окно (200), не смешан со старым оборотом",
                200.0, summary.getMean(), 0.0);
        assertEquals("timeMin — времена второго оборота", timeAt(1) + size * BIN, summary.getTimeMin());
        assertEquals("timeMax — времена второго оборота", timeAt(8) + size * BIN, summary.getTimeMax());
    }

    /**
     * ЧАСТИЧНЫЙ оборот — семантика скользящего окна: слоты, ещё не
     * переписанные новым оборотом, держат данные прежнего (сброс — на переписи,
     * а не на чтении); сумма и сводка — смесь «выживших» бин, без накопления.
     */
    @Test
    public void summary_PartialWrap_SlidingWindowMix() {
        final int size = 8;
        DataRowVO ring = new DataRowVO(size, BIN, "metric", "dataset");
        for (int i = 1; i <= size; i++) {
            ring.populate(10L, 100.0, timeAt(i));             // оборот 1 — полное кольцо
        }
        ring.populate(20L, 200.0, timeAt(1) + size * BIN);// оборот 2 — первые 2 слота
        ring.populate(20L, 200.0, timeAt(2) + size * BIN);

        DataRowSummary summary = ring.getSummary();
        assertEquals("6 бин прежнего оборота по 10 + 2 переписанных по 20",
                6L * 10L + 2L * 20L, summary.getCount());
        assertEquals("mean — средневзвешенное по выжившим бинам",
                (100.0 * 60L + 200.0 * 40L) / 100.0, summary.getMean(), 0.0);
        assertEquals("timeMin — старейший выживший бин (слот 3, оборот 1)", timeAt(3), summary.getTimeMin());
        assertEquals("timeMax — последняя отметка оборота 2", timeAt(2) + size * BIN, summary.getTimeMax());
    }
}
