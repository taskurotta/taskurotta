package ru.taskurotta.service.metrics;

import org.junit.Test;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataListener;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Валидация на старте: bin-секунды, не делящие окно,
 * и недостоверные лимиты → отказ от запуска (IllegalArgumentException в init()/конструкторе)
 * с понятным сообщением. Корректный конфиг стартует.
 */
public class MetricsStoreValidationTest {

    private static NumberDataListener noopListener() {
        return new NumberDataListener() {
            @Override
            public void handleNumberData(String metricName, String datasetName, Number value, long currentTime, int maxPoints) {
                // nothing
            }
        };
    }

    @Test
    public void hourBin_NotDividingHourWindow_RefusesStart() {
        // 7 с не делит 3600 (окно «последний час»)
        MetricsDataHandler handler = new MetricsDataHandler(7, 120, 1500, 86400, 600);
        try {
            handler.init();
            fail("ожидался IllegalArgumentException: hour bin 7 не делит 3600");
        } catch (IllegalArgumentException expected) {
            assertTrue("сообщение должно называть bin", expected.getMessage().contains("hour bin"));
        }
    }

    @Test
    public void dayBin_NotDividingDayWindow_RefusesStart() {
        // 121 с не делит 86400 (окно суток)
        MetricsDataHandler handler = new MetricsDataHandler(10, 121, 1500, 86400, 600);
        try {
            handler.init();
            fail("ожидался IllegalArgumentException: day bin 121 не делит 86400");
        } catch (IllegalArgumentException expected) {
            assertTrue("сообщение должно называть bin", expected.getMessage().contains("day bin"));
        }
    }

    @Test
    public void periodicBin_NotDividingDayWindow_RefusesStart() {
        // 7 с не делит 86400
        NumberDataHandler handler = new NumberDataHandler(7, 1500, 86400, 600);
        try {
            handler.init();
            fail("ожидался IllegalArgumentException: periodic bin 7 не делит 86400");
        } catch (IllegalArgumentException expected) {
            assertTrue("сообщение должно называть bin", expected.getMessage().contains("periodic bin"));
        }

        try {
            new MetricsFactory(1, 2, new MetricsDataHandler(), noopListener(), 121);
            fail("ожидался IllegalArgumentException: periodic bin 121 не делит 86400");
        } catch (IllegalArgumentException expected) {
            assertTrue("сообщение должно называть bin", expected.getMessage().contains("periodic bin"));
        }
    }

    @Test
    public void invalidLimitOrTtl_RefusesStart() {
        try {
            new MetricsDataHandler(10, 120, 0, 86400, 600).init();
            fail("ожидался IllegalArgumentException: max-rows = 0");
        } catch (IllegalArgumentException expected) {
            assertTrue("сообщение должно называть max-rows", expected.getMessage().contains("max-rows"));
        }

        try {
            new MetricsDataHandler(10, 120, 1500, 0, 600).init();
            fail("ожидался IllegalArgumentException: inactive-ttl = 0");
        } catch (IllegalArgumentException expected) {
            assertTrue("сообщение должно называть ttl", expected.getMessage().contains("inactive-ttl"));
        }

        try {
            new MetricsDataHandler(10, 120, 1500, 86400, 0).init();
            fail("ожидался IllegalArgumentException: status-log period = 0");
        } catch (IllegalArgumentException expected) {
            assertTrue("сообщение должно называть период", expected.getMessage().contains("status-log"));
        }
    }

    @Test
    public void validConfig_Starts() {
        // Дефолты (10/120, 60 — делят 3600/86400) — старт без исключения
        new MetricsDataHandler(10, 120, 1500, 86400, 600).init();
        new NumberDataHandler(60, 1500, 86400, 600).init();
    }
}
