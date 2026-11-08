package ru.taskurotta.dropwizard.resources.console.metrics;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Быстрый интеграционный тест «2 часа за минуту».
 *
 * Реальные компоненты без HTTP-слоя (в проекте нет embedded-server harness,
 * jetty/jersey не в тестовых зависимостях, сборка офлайн — HTTP-слой закрывает
 * полный E2E-прогон): реальные MetricsDataHandler/NumberDataHandler
 * с УСКОРЕННЫМИ свойствами (hour-bin-seconds=1 → кольцо 3600 бин,
 * day-bin-seconds=2 → кольцо 43200 бин, periodic.bin-seconds=1 → кольцо 86400 бин —
 * все делят окна 3600/86400, валидация не нарушена) и бэкдейтед
 * measurementTime через тот же в-процесс путь, что дампинг в проде (DataListener.handle).
 * Читающий путь — тот же класс MetricsDataProvider, что отдаёт GET /console/metrics/data.
 *
 * Данные (за секунды реального времени):
 * - «poll»/«poll»: 7200 отметок по 1 с = 2 ПОЛНЫХ оборота часового кольца (7200 с данных);
 * - «enqueue»/«enqueue»: 86400 отметок по 2 с = 2 полных оборота суточного (172800 с ≥ 86400);
 *   для суточного кольца два оборота обязательны: после ОДНОГО полного оборота
 *   дефект накопления ещё не проявлен (первые заполнения слотов), различие — только на переписи;
 * - idle-ряд «release»/«release»: одна старая отметка + свежие idle-дампинги (count=0);
 * - числовая «queueSize»: 200 выборок по 1 с (last-write-wins, оборотный дефект не применим).
 *
 * Семантика «now» (проверено в коде: бины по времени данных): кольцо адресовано
 * временем данных, а снимок полных бин и «окно» — wall-clock System.currentTimeMillis()
 * (getCompleteCounts(now) в хэндлерах). Данные постампованы до ≈ текущего момента
 * (обрываются в пределах последнего 2-с бина), поэтому «последний час/сутки» содержит
 * последние 3600/86400 с поинъектированных данных; исключается ровно один бин
 * binIndex(now) — недополненный, допуск (не более 1 бина).
 *
 * Целевая длительность теста — менее 60 с (замеряется внутри).
 */
public class MetricsFastWrapIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(MetricsFastWrapIntegrationTest.class);

    // Ускоренные свойства (см. default.properties: metric.data.method.hour-bin-seconds и др.)
    private static final int HOUR_BIN_SECONDS = 1;//кольцо 3600 бин
    private static final int DAY_BIN_SECONDS = 2;//кольцо 43200 бин
    private static final int PERIODIC_BIN_SECONDS = 1;//кольцо 86400 бин

    private static final long HOUR_WINDOW_MS = 3_600_000L;
    private static final long DAY_WINDOW_MS = 86_400_000L;

    // Пара «poll»/«poll»: 2 полных оборота часового кольца
    private static final String HOUR_METRIC = "poll";
    private static final int HOUR_EVENTS = 7200;//7200 с данных, шаг 1 с
    // Пара «enqueue»/«enqueue»: 2 полных оборота суточного кольца
    private static final String DAY_METRIC = "enqueue";
    private static final int DAY_EVENTS = 86_400;//172800 с данных, шаг 2 с
    // Idle-ряд
    private static final String IDLE_METRIC = "release";

    /** Минимальный пустой стаб, пока числовой путь не задействован в ветке method-метрики. */
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

    @Test
    public void twoHoursOfData_InUnderMinute_TotalsMatchInjected_NoWrapAccumulation() {
        long startMs = System.currentTimeMillis();

        // Реальные хэндлеры без init() (как в MetricsBinningTest): валидация «делит окно»
        // для ускоренных бин выполнена — 3600/1 и 86400/2 целые
        MetricsDataHandler method = new MetricsDataHandler(HOUR_BIN_SECONDS, DAY_BIN_SECONDS, 1500, 86400, 600);
        NumberDataHandler number = new NumberDataHandler(PERIODIC_BIN_SECONDS, 1500, 86400, 600);

        // Граница 2-с бина ≈ сейчас: все бины-слоты ниже вычислены от неё детерминированно
        long base = (System.currentTimeMillis() / 2000L) * 2000L;

        // ===== Генерация данных (за секунды реального времени) =====
        for (int i = 0; i < HOUR_EVENTS; i++) {
            method.handle(HOUR_METRIC, HOUR_METRIC, 1L, 0.0, base - 7_200_000L + i * 1000L);
        }
        for (int i = 0; i < DAY_EVENTS; i++) {
            method.handle(DAY_METRIC, DAY_METRIC, 1L, 0.0, base - 172_800_000L + i * 2000L);
        }
        // Старая отметка (48 ч до base, ещё на 60 с позади base): исключённый бин
        // binIndex(now) движется только вперёд от бина base, бин отметки лежит 60 с
        // позади — при любой длительности теста (ограничена 60 с) они не совпадают
        method.handle(IDLE_METRIC, IDLE_METRIC, 1L, 0.0, base - 172_860_000L);
        method.handle(IDLE_METRIC, IDLE_METRIC, 0L, Double.NaN, base - 120_000L);//idle-дампинги
        method.handle(IDLE_METRIC, IDLE_METRIC, 0L, Double.NaN, base - 60_000L);
        method.handle(IDLE_METRIC, IDLE_METRIC, 0L, Double.NaN, base - 1_000L);
        for (int i = 0; i < 200; i++) {
            number.handleNumberData("queueSize", "q", Integer.valueOf(i),
                    base - 200_000L + i * 1000L, 86_400);
        }

        // ===== РЕГРЕССИОННЫЙ (главный): после полного оборота сумма НЕ содержит
        // накопления предыдущих оборотов (класс дефекта E2E ringcompare-t120min) =====
        assertEquals("«всего за час» — только текущее окно (3600), не 7200 (2 оборота накоплены)",
                3600L, method.getTotalCountOfLastHour(HOUR_METRIC, HOUR_METRIC));
        assertEquals("«всего за сутки» — только текущее окно (43200), не 86400 (2 оборота накоплены)",
                43200L, method.getTotalCountOfLastDay(DAY_METRIC, DAY_METRIC));
        assertEquals("часовое кольцо суточной пары: 48 оборотов без накопления (3600 с / шаг 2 с = 1800), не 86400",
                1800L, method.getTotalCountOfLastHour(DAY_METRIC, DAY_METRIC));
        assertEquals("суточное кольцо часовой пары: оборота не было, всё поинъектированное на месте (7200)",
                7200L, method.getTotalCountOfLastDay(HOUR_METRIC, HOUR_METRIC));

        // ===== «Всего за окно» консоли = поинъектированному (допуск ≤ 1 бина) =====
        MetricsDataProvider provider = new MetricsDataProvider();
        provider.setMethodDataRetriever(method);
        provider.setNumberDataRetriever(number);
        provider.setMethodMetricsPeriodSeconds(1);
        provider.setPeriodicBinSeconds(PERIODIC_BIN_SECONDS);

        // Час: 3599 полных бин (3600−1), все времена — второго оборота, сумма = 3599;
        // + исключённый бин (1) = 3600 = поинъектированному за последние 3600 с данных
        DatasetVO hourDs = provider.getDataResponse(HOUR_METRIC, Arrays.asList(HOUR_METRIC), "local", "rate", "1hour").get(0);
        assertEquals("час — полные бины: 3600−1", 3599, nonNullCount(hourDs.getData()));
        assertEquals(3599L, sumNonNull(hourDs.getData()));
        assertWindow(hourDs.getData(), base - HOUR_WINDOW_MS, base - HOUR_WINDOW_MS + 1000L, base - 1000L, "час");

        // Суточки: 43199 полных бин (43200−1), все времена — второго оборота, сумма = 43199;
        // + исключённый бин (1) = 43200 = поинъектированному за последние 86400 с данных
        DatasetVO dayDs = provider.getDataResponse(DAY_METRIC, Arrays.asList(DAY_METRIC), "local", "rate", "24hours").get(0);
        assertEquals("сутки — полные бины: 43200−1", 43199, nonNullCount(dayDs.getData()));
        assertEquals(43199L, sumNonNull(dayDs.getData()));
        assertWindow(dayDs.getData(), base - DAY_WINDOW_MS, base - DAY_WINDOW_MS + 2000L, base - 2000L, "сутки");

        // ===== Idle-ряд: не растёт и не создаёт точек графика =====
        assertEquals("idle-ряд не растёт: 1 старая отметка, idle-дампинги (count=0) точек не пишут",
                1L, method.getTotalCountOfLastHour(IDLE_METRIC, IDLE_METRIC));
        DatasetVO idleDs = provider.getDataResponse(IDLE_METRIC, Arrays.asList(IDLE_METRIC), "local", "rate", "1hour").get(0);
        assertEquals("idle-ряд — одна точка (старая отметка), свежие idle-время точками не стали",
                1, nonNullCount(idleDs.getData()));
        assertEquals(base - 172_860_000L, idleDs.getData().get(1)[0].longValue());
        assertEquals("idle-дампинг «последнюю активность» не двигает",
                base - 172_860_000L, method.getLastActivityTimeMillis(IDLE_METRIC, IDLE_METRIC));

        // ===== Числовой путь (last-write-wins): подмножество period/шаг от реального кольца =====
        DatasetVO numberDs = provider.getDataResponse("queueSize", Arrays.asList("q"), "local", "size", "1hour").get(0);
        assertEquals("все 200 свежих выборок в подмножестве", 200, nonNullCount(numberDs.getData()));
        assertEquals("последняя выборка — свежайшая", 199, numberDs.getData().get(numberDs.getData().size() - 1)[1].intValue());
        assertEquals(base - 1000L, numberDs.getData().get(numberDs.getData().size() - 1)[0].longValue());

        long elapsedMs = System.currentTimeMillis() - startMs;
        logger.info("Fast wrap integration test finished in {} ms (target < 60000 ms)", elapsedMs);
        assertTrue("тест должен уложиться в 60 с (факт: " + elapsedMs + " мс)", elapsedMs < 60_000L);
    }

    /**
     * Все времена точек — в окне последнего оборота. Исключённый бин binIndex(now)
     * (wall-clock) в данных второго оборота несёт время ≤ maxAllowedExcluded —
     * отсюда допуск «min в [expectedMin, expectedMin + bin]».
     */
    private static void assertWindow(List<Number[]> points, long expectedMin, long maxAllowedMin, long expectedMax, String label) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Number[] p : points) {
            if (p[0] != null) {
                min = Math.min(min, p[0].longValue());
                max = Math.max(max, p[0].longValue());
            }
        }
        assertTrue("все времена точек [" + label + "] — в окне последнего оборота (min=" + min + ")",
                min >= expectedMin && min <= maxAllowedMin);
        assertEquals("последняя точка [" + label + "] — последний бин последнего оборота", expectedMax, max);
    }
}
