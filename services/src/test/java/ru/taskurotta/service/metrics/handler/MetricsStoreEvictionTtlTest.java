package ru.taskurotta.service.metrics.handler;

import org.junit.Test;
import ru.taskurotta.service.metrics.model.DataPointVO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Вытеснение по лимиту (только невидимые) и TTL-уборка.
 * Детерминизм: lastActive задаётся фиксированными measurementTime относительно
 * System.currentTimeMillis() с многократным запасом (границы не пересекаются
 * за время теста); purge вызывается напрямую (package-private), без ожидания периода.
 */
public class MetricsStoreEvictionTtlTest {

    private static final long HOUR = 3_600_000L;
    private static final long DAY = 86_400_000L;

    /**
     * Лимит 3: при создании 4-й строки вытесняется самая старая НЕВИДИМАЯ
     * (час: lastActive < now−1 ч). Видимые (активные) и невидимая помладше — не тронуты.
     * Вытеснение по хранилищам раздельное: суточное окно невидимости — 24 ч,
     * строка 3-часовой давности в суточном хранилище ещё видима (удаляется TTL-уборкой).
     */
    @Test
    public void overLimit_EvictsOldestInvisibleOnly() {
        long now = System.currentTimeMillis();
        MetricsDataHandler handler = new MetricsDataHandler(10, 120, 3, 86400, 600);

        handler.handle("m1", "d", 1L, 0.0, now - 2 * HOUR);//невидимая (младшая)
        handler.handle("m2", "d", 1L, 0.0, now - 3 * HOUR);//невидимая (старшая) — жертва
        handler.handle("m3", "d", 1L, 0.0, now - 10_000L);//активная
        handler.handle("m4", "d", 1L, 0.0, now - 20_000L);//активная; size=4 > 3

        assertNull("самая старая невидимая вытеснена из часового", handler.getCountsForLastHour("m2", "d"));
        assertNotNull("суточное окно 24 ч: строка ещё видима", handler.getCountsForLastDay("m2", "d"));
        assertNotNull("невидимая помладше осталась", handler.getCountsForLastHour("m1", "d"));
        assertNotNull("активные не тронуты", handler.getCountsForLastHour("m3", "d"));
        assertNotNull(handler.getCountsForLastHour("m4", "d"));
    }

    /**
     * Невидимых нет (все активны — edge case спеки: 1611 активных > 1500):
     * вытеснение пропущено, строки не удаляются, счётчик skip растёт.
     */
    @Test
    public void overLimit_NoInvisible_SkipsEviction() {
        long now = System.currentTimeMillis();
        MetricsDataHandler handler = new MetricsDataHandler(10, 120, 3, 86400, 600);

        handler.handle("m1", "d", 1L, 0.0, now - 10_000L);
        handler.handle("m2", "d", 1L, 0.0, now - 20_000L);
        handler.handle("m3", "d", 1L, 0.0, now - 30_000L);
        handler.handle("m4", "d", 1L, 0.0, now - 40_000L);//size=4 > 3, невидимых нет

        // Счётчик — по хранилищам: часовое и суточное каждое пропуски считают
        assertEquals("вытеснение пропущено (час + сутки)", 2L, handler.evictSkipped.get());
        assertEquals("ничего не вытеснено", 0L, handler.evictedRows.get());
        assertNotNull(handler.getCountsForLastHour("m1", "d"));
        assertNotNull(handler.getCountsForLastHour("m2", "d"));
        assertNotNull(handler.getCountsForLastHour("m3", "d"));
        assertNotNull(handler.getCountsForLastHour("m4", "d"));
    }

    /**
     * Числовое хранилище: лимит 2, невидимые — старше 24 ч; вытесняется самая старая.
     */
    @Test
    public void numberOverLimit_EvictsOldestInvisibleOnly() {
        long now = System.currentTimeMillis();
        NumberDataHandler handler = new NumberDataHandler(60, 2, 86400, 600);

        handler.handleNumberData("q1", "d", Integer.valueOf(1), now - 25 * 3_600_000L, 1440);
        handler.handleNumberData("q2", "d", Integer.valueOf(1), now - 30 * 3_600_000L, 1440);//жертва
        handler.handleNumberData("q3", "d", Integer.valueOf(1), now - 10_000L, 1440);

        assertNull("самая старая невидимая вытеснена", handler.getData("q2", "d"));
        assertNotNull(handler.getData("q1", "d"));
        assertNotNull(handler.getData("q3", "d"));
    }

    /**
     * TTL (короткое в тесте — 60 с): строка без данных дольше TTL удаляется
     * из часового и суточного хранилищ разом; активная остаётся.
     */
    @Test
    public void ttlPurge_RemovesInactiveRows() {
        long now = System.currentTimeMillis();
        MetricsDataHandler handler = new MetricsDataHandler(10, 120, 1500, 60, 600);

        handler.handle("old", "d", 1L, 0.0, now - 120_000L);//неактивна 120 с > TTL 60 с
        handler.handle("new", "d", 1L, 0.0, now - 10_000L);//активна

        handler.purgeInactiveAndLogStatus();

        assertNull("часовая строка удалена", handler.getCountsForLastHour("old", "d"));
        assertNull("суточная строка удалена разом", handler.getCountsForLastDay("old", "d"));
        assertNotNull("активная не удалена", handler.getCountsForLastHour("new", "d"));
    }

    /**
     * Воссоздание после TTL: следующая отметка строит строку заново —
     * полный счётчик с контрольной точки, без дублей.
     */
    @Test
    public void ttlPurge_NextMarkRecreatesWithFullCounter() {
        long now = System.currentTimeMillis();
        MetricsDataHandler handler = new MetricsDataHandler(10, 120, 1500, 60, 600);

        handler.handle("old", "d", 1L, 0.0, now - 120_000L);
        handler.purgeInactiveAndLogStatus();
        assertNull(handler.getCountsForLastHour("old", "d"));

        handler.handle("old", "d", 5L, 1.0, now);

        assertEquals("счётчик полный, без дублей", 5L, handler.getTotalCountOfLastHour("old", "d"));
        assertEquals(now, handler.getLastActivityTimeMillis("old", "d"));
    }

    /**
     * TTL числового хранилища.
     */
    @Test
    public void numberTtlPurge_RemovesInactiveRows() {
        long now = System.currentTimeMillis();
        NumberDataHandler handler = new NumberDataHandler(60, 1500, 60, 600);

        handler.handleNumberData("old", "d", Integer.valueOf(1), now - 120_000L, 1440);
        handler.handleNumberData("new", "d", Integer.valueOf(1), now - 10_000L, 1440);

        handler.purgeInactiveAndLogStatus();

        assertNull(handler.getData("old", "d"));
        assertNotNull(handler.getData("new", "d"));
    }

    @Test
    public void purge_DoesNotTouchActiveRows_KeepsData() {
        // Контроль: purge не теряет данные активных строк
        long now = System.currentTimeMillis();
        MetricsDataHandler handler = new MetricsDataHandler(10, 120, 1500, 60, 600);

        handler.handle("active", "d", 7L, 0.0, now - 5_000L);
        handler.purgeInactiveAndLogStatus();

        DataPointVO<Long>[] points = handler.getCountsForLastHour("active", "d");
        assertNotNull(points);
        long total = handler.getTotalCountOfLastHour("active", "d");
        assertTrue("данные активной строки не потеряны", total >= 7L);
    }
}
