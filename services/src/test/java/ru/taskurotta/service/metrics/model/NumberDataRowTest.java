package ru.taskurotta.service.metrics.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Ряд с ЕДИНСТВЕННОЙ записанной точкой (окно между первой и второй выборкой)
 * отдаёт из getLastValue() значение записанной точки — без ошибки.
 * Конструктор ряда принимает размер бина (1 с — шаг выборки в этом тесте);
 * getLastValue = значение бина последней записи.
 */
public class NumberDataRowTest {

    // Фиксированные константы времени и значения — детерминизм, без wall-clock ассертов
    private static final int RING_SIZE = 8;
    private static final long BIN_MILLIS = 1000L;//1-с бин: шаг выборки DT = граница бина
    private static final long T0 = 1_000_000L;
    private static final long DT = 1000L;
    private static final long FIRST_SAMPLE_TIME = T0;
    private static final int FIRST_SAMPLE_VALUE = 42;

    @Test
    public void getLastValue_OnePointOnly_ReturnsPointValue() {
        NumberDataRowVO row = new NumberDataRowVO(RING_SIZE, BIN_MILLIS, "queueSize", "queue");

        // одна выборка — окно между первой и второй выборкой
        row.populate(FIRST_SAMPLE_VALUE, FIRST_SAMPLE_TIME);

        assertEquals(FIRST_SAMPLE_VALUE, row.getLastValue());
    }

    /**
     * getLastValue = самое свежее записанное значение; при незаполненном
     * последнем интервале (2+ точки в ряду) — значение предыдущей точки; поведение после wrap.
     * Случай ряда с ОДНОЙ точкой НЕ дублируется — он покрыт NPE-тестом выше.
     */
    @Test
    public void populateAndLastValue() {
        // 1. getLastValue = самое свежее записанное значение (время возрастает)
        NumberDataRowVO row = new NumberDataRowVO(RING_SIZE, BIN_MILLIS, "queueSize", "queue");
        row.populate(10, T0);
        row.populate(20, T0 + DT);
        row.populate(30, T0 + 2 * DT);
        assertEquals(30, row.getLastValue());

        // 2. Незаполненный последний интервал: точка текущего слота не новее предыдущей
        //    (держит старое время — интервал ещё не «свежий») → значение предыдущей точки
        NumberDataRowVO staleRow = new NumberDataRowVO(RING_SIZE, BIN_MILLIS, "queueSize", "queue");
        staleRow.populate(10, T0);
        staleRow.populate(20, T0 + DT);
        staleRow.populate(15, T0); // точка последнего слота старее предыдущей
        assertEquals(20, staleRow.getLastValue());

        // 3. Поведение после wrap кольца: size+2 вызова populate на кольце size=8
        NumberDataRowVO wrapRow = new NumberDataRowVO(RING_SIZE, BIN_MILLIS, "queueSize", "queue");
        for (int i = 1; i <= RING_SIZE + 2; i++) {
            wrapRow.populate(i, T0 + (i - 1) * DT);
        }
        // самое свежее записанное значение — 10-е
        assertEquals(10, wrapRow.getLastValue());
    }
}
