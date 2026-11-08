package ru.taskurotta.service.metrics.handler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Статус-лог: периодическая задача (TTL-уборка + статус-строка)
 * считает события фактом (rows/evicted/purged/evictSkipped) и сбрасывает счётчики
 * после отсчёта. Задача вызывается напрямую (package-private) — без ожидания
 * реального 10-минутного периода; строка сама пишется в лог (видна в локальном
 * старте и в e2e-логe).
 */
public class MetricsStoreStatusLogTest {

    /**
     * Счётчики по факту событий: evictSkipped (нет невидимых при превышении лимита)
     * и purged (TTL-удаление) — видны до сброса; после прогона задачи — обнулены.
     */
    @Test
    public void statusLog_CountersByFacts_AndResetAfterLog() {
        long now = System.currentTimeMillis();

        // 1. Событие «вытеснение пропущено»: 4 активные строки при лимите 3
        MetricsDataHandler overLimitHandler = new MetricsDataHandler(10, 120, 3, 86400, 600);
        overLimitHandler.handle("a1", "d", 1L, 0.0, now - 10_000L);
        overLimitHandler.handle("a2", "d", 1L, 0.0, now - 20_000L);
        overLimitHandler.handle("a3", "d", 1L, 0.0, now - 30_000L);
        overLimitHandler.handle("a4", "d", 1L, 0.0, now - 40_000L);
        // Счётчик — по хранилищам: часовое и суточное каждое считают пропуск
        assertEquals("evictSkipped по факту (час + сутки)", 2L, overLimitHandler.evictSkipped.get());

        // 2. Событие «TTL-удаление»: неактивная строка (TTL 60 с)
        MetricsDataHandler ttlHandler = new MetricsDataHandler(10, 120, 1500, 60, 600);
        ttlHandler.handle("stale", "d", 1L, 0.0, now - 120_000L);
        ttlHandler.handle("fresh", "d", 1L, 0.0, now - 10_000L);

        // 3. Прогон задачи: статус-строка пишется, счётчики сбрасываются
        overLimitHandler.purgeInactiveAndLogStatus();
        ttlHandler.purgeInactiveAndLogStatus();

        assertEquals("evictSkipped сброшен после статуса", 0L, overLimitHandler.evictSkipped.get());
        assertEquals("evicted сброшен", 0L, ttlHandler.evictedRows.get());
        assertEquals("purged сброшен", 0L, ttlHandler.purgedRows.get());

        // 4. Уборка по факту: неактивная удалена, активные на месте
        assertNull("TTL-строка удалена", ttlHandler.getCountsForLastHour("stale", "d"));
        assertNotNull("активная строка на месте", ttlHandler.getCountsForLastHour("fresh", "d"));
        assertNotNull("лимитный кейс: строки не тронуты (невидимых не было)",
                overLimitHandler.getCountsForLastHour("a1", "d"));
    }

    /**
     * Статус-лог числового хранилища: свой счётчик и своя уборка.
     */
    @Test
    public void numberStatusLog_CountersByFacts_AndResetAfterLog() {
        long now = System.currentTimeMillis();
        NumberDataHandler handler = new NumberDataHandler(60, 1500, 60, 600);

        handler.handleNumberData("stale", "d", Integer.valueOf(1), now - 120_000L, 1440);
        handler.handleNumberData("fresh", "d", Integer.valueOf(1), now - 10_000L, 1440);

        handler.purgeInactiveAndLogStatus();

        assertEquals("purged сброшен после статуса", 0L, handler.purgedRows.get());
        assertEquals("evictSkipped по нулям", 0L, handler.evictSkipped.get());
        assertNull(handler.getData("stale", "d"));
        assertNotNull(handler.getData("fresh", "d"));
    }
}
