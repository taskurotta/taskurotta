package ru.taskurotta.service.metrics.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.retriever.metrics.MetricsNumberDataRetriever;
import ru.taskurotta.service.metrics.MetricsDataUtils;
import ru.taskurotta.service.metrics.TimeConstants;
import ru.taskurotta.service.metrics.model.DataPointVO;
import ru.taskurotta.service.metrics.model.NumberDataRowVO;
import ru.taskurotta.util.Shutdown;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handler for metrics with simple numeric data. Stores and exposes data.
 * User: dimadin
 * Date: 25.10.13 10:30
 */
public class NumberDataHandler implements NumberDataListener, MetricsNumberDataRetriever, TimeConstants {

    private static NumberDataHandler singleton;
    private static final Logger logger = LoggerFactory.getLogger(NumberDataHandler.class);

    private Map<String, NumberDataRowVO> dataHolder = new ConcurrentHashMap<>();

    /** Агрегационный интервал бина, секунд; окно кольца — 24 часа (ring = 86400/bin). */
    private final int periodicBinSeconds;
    private final int ringSize;
    /** Жёсткий лимит строк хранилища. */
    private final int maxRows;
    /** Срок неактивности строки до удаления, секунд. */
    private final int inactiveTtlSeconds;
    private final int statusLogPeriodSeconds;

    //package-private: юнит-тесты purger/статус-лога читают счётчики напрямую
    final AtomicLong evictedRows = new AtomicLong();
    final AtomicLong purgedRows = new AtomicLong();
    final AtomicLong evictSkipped = new AtomicLong();

    private ScheduledExecutorService statusExecutor;

    /**
     * periodicBinSeconds — агрегационный интервал бина (24-ч окно); maxRows — жёсткий
     * лимит строк; inactiveTtlSeconds — TTL неактивных строк; statusLogPeriodSeconds —
     * период уборки/статус-лога.
     */
    public NumberDataHandler(int periodicBinSeconds, int maxRows,
                             int inactiveTtlSeconds, int statusLogPeriodSeconds) {
        this.periodicBinSeconds = periodicBinSeconds;
        this.ringSize = SECONDS_IN_24_HOURS / periodicBinSeconds;
        this.maxRows = maxRows;
        this.inactiveTtlSeconds = inactiveTtlSeconds;
        this.statusLogPeriodSeconds = statusLogPeriodSeconds;

        this.statusExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("NumberMetrics-status");
                return thread;
            }
        });
        Shutdown.addHook(statusExecutor);
    }

    public NumberDataHandler() {
        this(60, 1500, 86400, 600);
    }

    private volatile boolean initialized = false;

    @PostConstruct
    public void init() {
        //XML-бин объявляет init-method="init" и @PostConstruct: защита от двойного запуска задачи
        if (initialized) {
            return;
        }
        initialized = true;

        validateProperties();
        singleton = this;

        statusExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    purgeInactiveAndLogStatus();
                } catch (Throwable e) {//scheduled pool shuts quietly on exception
                    logger.error("Cannot run number metrics stores status task", e);
                }
            }
        }, statusLogPeriodSeconds, statusLogPeriodSeconds, TimeUnit.SECONDS);
    }

    /**
     * Валидация конфига: неверный бин/лимит — отказ от запуска с понятным сообщением.
     */
    private void validateProperties() {
        if (periodicBinSeconds <= 0) {
            throw new IllegalArgumentException("periodic bin seconds must be positive: " + periodicBinSeconds);
        }
        if (SECONDS_IN_24_HOURS % periodicBinSeconds != 0) {
            throw new IllegalArgumentException("periodic bin must divide the 24-hour window: periodicBinSeconds=" + periodicBinSeconds);
        }
        if (maxRows <= 0) {
            throw new IllegalArgumentException("number metrics store max-rows must be positive: " + maxRows);
        }
        if (inactiveTtlSeconds <= 0) {
            throw new IllegalArgumentException("number metrics store inactive-ttl must be positive: " + inactiveTtlSeconds);
        }
        if (statusLogPeriodSeconds <= 0) {
            throw new IllegalArgumentException("number metrics status-log period must be positive: " + statusLogPeriodSeconds);
        }
    }

    @Override
    public void handleNumberData(String metricName, String datasetName, Number value, long currentTime, int maxPoints) {
        String holderKey = MetricsDataUtils.getKey(metricName, datasetName);

        NumberDataRowVO dataRow = dataHolder.get(holderKey);
        if (dataRow == null) {
            synchronized (dataHolder) {
                dataRow = dataHolder.get(holderKey);
                if (dataRow == null) {
                    dataRow = new NumberDataRowVO(ringSize, periodicBinSeconds * 1000L, metricName, datasetName);
                    dataHolder.put(holderKey, dataRow);
                    evictInvisibleIfOverLimit(holderKey, System.currentTimeMillis());
                }
            }
        }

        int position = dataRow.populate(value, currentTime);

        logger.debug("Handled [{}] data for bin [{}], metric[{}], dataset[{}], value[{}], measurementTime[{}]", value.getClass(), position, metricName, datasetName, value, currentTime);
    }

    /**
     * При превышении лимита вытесняются строки с самым старым lastActive СРЕДИ
     * НЕВИДИМЫХ (lastActive старше 24 часов); видимые (активные) строки не вытесняются.
     * Невидимых кандидатов нет — вытеснение пропускается (видно в статус-логе).
     */
    private void evictInvisibleIfOverLimit(String newKey, long now) {
        long invisibleBefore = now - SECONDS_IN_24_HOURS * 1000L;
        while (dataHolder.size() > maxRows) {
            String victimKey = null;
            long victimLastActive = Long.MAX_VALUE;
            for (Map.Entry<String, NumberDataRowVO> entry : dataHolder.entrySet()) {
                if (entry.getKey().equals(newKey)) {
                    continue;
                }
                long lastActive = entry.getValue().getLatestActivity();
                if (lastActive < invisibleBefore && lastActive < victimLastActive) {
                    victimKey = entry.getKey();
                    victimLastActive = lastActive;
                }
            }
            if (victimKey == null) {
                evictSkipped.incrementAndGet();
                return;
            }
            dataHolder.remove(victimKey);
            evictedRows.incrementAndGet();
            logger.info("Evicted invisible number metrics row [{}] (lastActive [{}]) to enforce store limit [{}]", victimKey, victimLastActive, maxRows);
        }
    }

    /**
     * Периодическая уборка (совпадает со статус-логом): TTL — строка без данных дольше
     * inactiveTtlSeconds удаляется; счётчики метрик не удаляются.
     * package-private: юнит-тесты вызывают напрямую (без ожидания реального периода).
     */
    void purgeInactiveAndLogStatus() {
        long now = System.currentTimeMillis();
        long inactiveBefore = now - inactiveTtlSeconds * 1000L;

        int purged = 0;
        for (Map.Entry<String, NumberDataRowVO> entry : new ArrayList<Map.Entry<String, NumberDataRowVO>>(dataHolder.entrySet())) {
            if (entry.getValue().getLatestActivity() < inactiveBefore) {
                if (dataHolder.remove(entry.getKey()) != null) {
                    purged++;
                }
            }
        }
        purgedRows.addAndGet(purged);

        long evicted = evictedRows.getAndSet(0);
        long purgedTotal = purgedRows.getAndSet(0);
        long skipped = evictSkipped.getAndSet(0);

        Runtime runtime = Runtime.getRuntime();
        long usedKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
        long committedKb = runtime.totalMemory() / 1024;
        long maxKb = runtime.maxMemory() / 1024;

        logger.info("Number metrics stores status: rows[{}], evicted[{}], purged[{}], evictSkipped[{}] since last status log; JVM heap used[{}]KB committed[{}]KB max[{}]KB",
                dataHolder.size(), evicted, purgedTotal, skipped, usedKb, committedKb, maxKb);
        if (skipped > 0) {
            logger.warn("Number metrics store row limit [{}] exceeded but no invisible rows to evict (skipped [{}])", maxRows, skipped);
        }
    }

    @Override
    public Collection<String> getNumberMetricNames() {
        Set<String> result = new HashSet<>();
        for (NumberDataRowVO value : dataHolder.values()) {
            result.add(value.getMetricsName());
        }
        return result;
    }

    @Override
    public Collection<String> getNumberDataSets(String metricName) {
        Set<String> result = new HashSet<>();
        for (NumberDataRowVO value : dataHolder.values()) {
            if (value.getMetricsName().equals(metricName)) {
                result.add(value.getDataSetName());
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Number>[] getData(String metricName, String datasetName) {
        DataPointVO<Number>[] result = null;
        NumberDataRowVO value = dataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            result = value.getCompleteData(System.currentTimeMillis());
        }

        return result;
    }

    @Override
    public Date getLastActivityTime(String metricName, String datasetName) {
        Date result = null;
        NumberDataRowVO value = dataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            result = new Date(value.getLatestActivity());
        }
        return result;
    }

    @Override
    public Number getLastValue(String metricName, String datasetName) {
        Number result = null;
        NumberDataRowVO value = dataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            result = value.getLastValue();
        }
        logger.debug("Last value for metric[{}], dataset[{}] is [{}]", metricName, datasetName, result);
        return result;
    }

    public static NumberDataHandler getInstance() {
        return singleton;
    }

    public void shutdown() {
        statusExecutor.shutdown();
    }
}
