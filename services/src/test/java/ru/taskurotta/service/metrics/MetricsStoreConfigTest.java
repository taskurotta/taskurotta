package ru.taskurotta.service.metrics;

import org.junit.Test;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Настраиваемые свойства хранилища метрик:
 * дефолты, зафиксированные в taskurotta/src/main/resources/default.properties,
 * совпадают с дефолтами кода; размеры колец — функция bin-секунд (360/720/1440 —
 * выводятся из свойств, не пинённые константы, data-model.md §3).
 *
 * Детерминизм: фиксированные временные константы; длина
 * «полных бин» (size-1) от wall-clock не зависит (T0 — далёкое прошлое,
 * текущий бин — всегда отдельный слот).
 */
public class MetricsStoreConfigTest {

    // T0 — фиксированное время в далёком прошлом, кратное любому бину (мс)
    private static final long T0 = 1_000_000_000L;

    /** Путь к default.properties приложения (surefire работает из basedir модуля services). */
    private static Properties loadAppDefaults() {
        File defaults = new File("..", "taskurotta/src/main/resources/default.properties");
        assertTrue("default.properties приложения не найден по пути " + defaults, defaults.isFile());
        Properties properties = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(defaults);
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + defaults, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // nothing to do
                }
            }
        }
        return properties;
    }

    @Test
    public void defaults_MatchRepositoryDefaults() {
        Properties defaults = loadAppDefaults();

        assertEquals("10", defaults.getProperty("metric.data.method.hour-bin-seconds"));
        assertEquals("120", defaults.getProperty("metric.data.method.day-bin-seconds"));
        assertEquals("60", defaults.getProperty("metric.data.periodic.bin-seconds"));
        assertEquals("1500", defaults.getProperty("metric.data.store.max-rows"));
        assertEquals("86400", defaults.getProperty("metric.data.store.inactive-ttl-seconds"));
        assertEquals("600", defaults.getProperty("metric.data.status-log-period-seconds"));

        // Дефолты кода (no-arg конструкторы) — те же значения: размеры колец
        // 360/720/1440 → длина полных бин 359/719/1439
        MetricsDataHandler methodHandler = new MetricsDataHandler();
        methodHandler.handle("metricA", "dsA", 1L, 1.0, T0);
        assertEquals(359, methodHandler.getCountsForLastHour("metricA", "dsA").length);
        assertEquals(719, methodHandler.getCountsForLastDay("metricA", "dsA").length);

        NumberDataHandler numberHandler = new NumberDataHandler();
        numberHandler.handleNumberData("queueSize", "dsB", Integer.valueOf(1), T0, 1440);
        assertEquals(1439, numberHandler.getData("queueSize", "dsB").length);
    }

    @Test
    public void ringSize_IsFunctionOfBinSeconds_NotConstants() {
        // Час: бин 60 с → кольцо 3600/60 = 60 бин (359 полных точек), а не константа 360.
        MetricsDataHandler customHandler = new MetricsDataHandler(60, 3600, 1500, 86400, 600);
        customHandler.handle("metricB", "dsA", 1L, 1.0, T0);
        assertEquals(59, customHandler.getCountsForLastHour("metricB", "dsA").length);

        // Сутки: бин 3600 с → кольцо 86400/3600 = 24 бин (23 полных точки).
        assertEquals(23, customHandler.getCountsForLastDay("metricB", "dsA").length);

        // Периодические: бин 300 с → кольцо 86400/300 = 288 бин (287 полных точек).
        NumberDataHandler customNumber = new NumberDataHandler(300, 1500, 86400, 600);
        customNumber.handleNumberData("queueSize", "dsC", Integer.valueOf(1), T0, 288);
        assertEquals(287, customNumber.getData("queueSize", "dsC").length);
    }

    /**
     * 4-arg конструктор MetricsFactory (дефолт bin = 60 с): размер кольца периодической
     * метрики = 86400/60 = 1440 — НЕ 86400/период (17280 при периоде 5 с).
     */
    @Test
    public void factoryDefault_PeriodicRingFromBinSeconds() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<Integer> receivedMaxPoints = new ArrayList<Integer>();

        NumberDataHandler numberHandler = new NumberDataHandler();
        MetricsFactory factory = new MetricsFactory(1, 2, new MetricsDataHandler(), new ru.taskurotta.service.metrics.handler.NumberDataListener() {
            @Override
            public void handleNumberData(String metricName, String datasetName, Number value, long currentTime, int maxPoints) {
                synchronized (receivedMaxPoints) {
                    receivedMaxPoints.add(maxPoints);
                }
                latch.countDown();
            }
        });

        PeriodicMetric metric = factory.getPeriodicInstance("queueSize", 5);
        metric.periodicMark(new PeriodicMetric.DatasetValueExtractor() {
            @Override
            public List<String> getDatasets() {
                return Arrays.asList("q1");
            }

            @Override
            public Number getDatasetValue(String dataset) {
                return Integer.valueOf(1);
            }

            @Override
            public Number getGeneralValue(Map<String, Number> datasetsValues) {
                return null;
            }
        });

        assertTrue("недождались первой выборки", latch.await(10, TimeUnit.SECONDS));
        synchronized (receivedMaxPoints) {
            assertEquals(1, receivedMaxPoints.size());
            assertEquals(Integer.valueOf(1440), receivedMaxPoints.get(0));
        }
        factory.shutdown();
    }
}
