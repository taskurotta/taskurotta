package ru.taskurotta.service.metrics.handler;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.service.metrics.MetricsDataUtils;
import ru.taskurotta.service.metrics.TimeConstants;
import ru.taskurotta.service.metrics.model.DataPointVO;
import ru.taskurotta.service.metrics.model.DataRowVO;
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
 * Holds metrics data collected via backend services methods invocations. Stores and exposes data.
 * Date: 19.09.13 10:30
 */
public class MetricsDataHandler implements DataListener, MetricsMethodDataRetriever, TimeConstants {
    private static MetricsDataHandler singleton;

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataHandler.class);

    /** Агрегационный интервал бина, секунд. Окна: час — 3600 s, сутки — 86400 s. */
    private final int hourBinSeconds;
    private final int dayBinSeconds;
    private final int hourBins;
    private final int dayBins;
    /** Жёсткий лимит строк на хранилище. */
    private final int maxRows;
    /** Срок неактивности строки до удаления, секунд. */
    private final int inactiveTtlSeconds;
    private final int statusLogPeriodSeconds;

    private Map<String, DataRowVO> lastHourDataHolder = new ConcurrentHashMap<>();
    private Map<String, DataRowVO> lastDayDataHolder = new ConcurrentHashMap<>();

    //package-private: юнит-тесты purger/статус-лога читают счётчики напрямую
    final AtomicLong evictedRows = new AtomicLong();
    final AtomicLong purgedRows = new AtomicLong();
    final AtomicLong evictSkipped = new AtomicLong();

    private ScheduledExecutorService statusExecutor;

    /**
     * hourBinSeconds/dayBinSeconds — агрегационный интервал бина; окна хранилищ:
     * час — 3600 s, сутки — 86400 s. maxRows — жёсткий лимит строк на хранилище;
     * inactiveTtlSeconds — срок неактивности строки до удаления;
     * statusLogPeriodSeconds — период периодической уборки (TTL) и статус-лога.
     */
    public MetricsDataHandler(int hourBinSeconds, int dayBinSeconds, int maxRows,
                              int inactiveTtlSeconds, int statusLogPeriodSeconds) {
        this.hourBinSeconds = hourBinSeconds;
        this.dayBinSeconds = dayBinSeconds;
        this.hourBins = SECONDS_IN_HOUR / hourBinSeconds;
        this.dayBins = SECONDS_IN_24_HOURS / dayBinSeconds;
        this.maxRows = maxRows;
        this.inactiveTtlSeconds = inactiveTtlSeconds;
        this.statusLogPeriodSeconds = statusLogPeriodSeconds;

        this.statusExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("Metrics-status");
                return thread;
            }
        });
        Shutdown.addHook(statusExecutor);
    }

    public MetricsDataHandler() {
        this(10, 120, 1500, 86400, 600);
    }

    public static MetricsDataHandler getInstance() {
        return singleton;
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
                    logger.error("Cannot run metrics stores status task", e);
                }
            }
        }, statusLogPeriodSeconds, statusLogPeriodSeconds, TimeUnit.SECONDS);
    }

    /**
     * Валидация конфига: неверный бин/лимит — отказ от запуска с понятным сообщением.
     */
    private void validateProperties() {
        if (hourBinSeconds <= 0 || dayBinSeconds <= 0) {
            throw new IllegalArgumentException("metrics bin seconds must be positive: hourBinSeconds=" + hourBinSeconds + ", dayBinSeconds=" + dayBinSeconds);
        }
        if (SECONDS_IN_HOUR % hourBinSeconds != 0) {
            throw new IllegalArgumentException("hour bin must divide the 1-hour window: hourBinSeconds=" + hourBinSeconds);
        }
        if (SECONDS_IN_24_HOURS % dayBinSeconds != 0) {
            throw new IllegalArgumentException("day bin must divide the 24-hour window: dayBinSeconds=" + dayBinSeconds);
        }
        if (maxRows <= 0) {
            throw new IllegalArgumentException("metrics store max-rows must be positive: " + maxRows);
        }
        if (inactiveTtlSeconds <= 0) {
            throw new IllegalArgumentException("metrics store inactive-ttl must be positive: " + inactiveTtlSeconds);
        }
        if (statusLogPeriodSeconds <= 0) {
            throw new IllegalArgumentException("metrics status-log period must be positive: " + statusLogPeriodSeconds);
        }
    }

    @Override
    //triggers approximately once per second
    public void handle(String metricName, String datasetName, long count, double mean, long measurementTime) {
        //idle-дамп (count=0): строку не создаёт и точку не пишет — неактивный ключ ряд не порождает
        if (count <= 0) {
            return;
        }

        String holderKey = MetricsDataUtils.getKey(metricName, datasetName);

        DataRowVO hourData = getOrCreateRow(lastHourDataHolder, holderKey, metricName, datasetName,
                hourBins, hourBinSeconds * 1000L, SECONDS_IN_HOUR * 1000L);
        hourData.populate(count, mean, measurementTime);

        DataRowVO dayData = getOrCreateRow(lastDayDataHolder, holderKey, metricName, datasetName,
                dayBins, dayBinSeconds * 1000L, SECONDS_IN_24_HOURS * 1000L);
        dayData.populate(count, mean, measurementTime);

        logger.trace("Handled data for metric[{}], dataset[{}], count[{}], mean[{}], measurementTime[{}]", metricName, datasetName, count, mean, measurementTime);
    }

    /**
     * Создание строки (первая отметка count>0): при превышении лимита вытесняются
     * строки с самым старым lastActive СРЕДИ НЕВИДИМЫХ (для хранилища «последний час» —
     * lastActive старше 1 часа, для суточного — старше 24 часов); видимые (активные)
     * строки не вытесняются никогда. Невидимых кандидатов нет — вытеснение пропускается
     * (событие видно в статус-логе).
     */
    private DataRowVO getOrCreateRow(final Map<String, DataRowVO> holder, String holderKey,
                                     String metricName, String datasetName, int size, long binMillis,
                                     long invisibleWindowMillis) {
        DataRowVO dataRow = holder.get(holderKey);
        if (dataRow == null) {
            synchronized (holder) {
                dataRow = holder.get(holderKey);
                if (dataRow == null) {
                    dataRow = new DataRowVO(size, binMillis, metricName, datasetName);
                    holder.put(holderKey, dataRow);
                    evictInvisibleIfOverLimit(holder, holderKey, System.currentTimeMillis(), invisibleWindowMillis);
                }
            }
        }
        return dataRow;
    }

    private void evictInvisibleIfOverLimit(Map<String, DataRowVO> holder, String newKey, long now, long invisibleWindowMillis) {
        long invisibleBefore = now - invisibleWindowMillis;
        while (holder.size() > maxRows) {
            String victimKey = null;
            long victimLastActive = Long.MAX_VALUE;
            for (Map.Entry<String, DataRowVO> entry : holder.entrySet()) {
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
            holder.remove(victimKey);
            evictedRows.incrementAndGet();
            logger.info("Evicted invisible metrics row [{}] (lastActive [{}]) to enforce store limit [{}]", victimKey, victimLastActive, maxRows);
        }
    }

    /**
     * Периодическая уборка (совпадает со статус-логом): TTL — строка без данных дольше
     * inactiveTtlSeconds удаляется. Методический ключ удаляется из часового и суточного
     * хранилищ разом; ключи-счётчики (CheckPoint) не удаляются.
     * package-private: юнит-тесты вызывают напрямую (без ожидания реального периода).
     */
    void purgeInactiveAndLogStatus() {
        long now = System.currentTimeMillis();
        long inactiveBefore = now - inactiveTtlSeconds * 1000L;

        Set<String> purgedKeys = new HashSet<String>();
        for (Map.Entry<String, DataRowVO> entry : new ArrayList<Map.Entry<String, DataRowVO>>(lastHourDataHolder.entrySet())) {
            if (entry.getValue().getLatestActivity() < inactiveBefore) {
                if (lastHourDataHolder.remove(entry.getKey()) != null) {
                    purgedKeys.add(entry.getKey());
                    lastDayDataHolder.remove(entry.getKey());
                }
            }
        }
        for (Map.Entry<String, DataRowVO> entry : new ArrayList<Map.Entry<String, DataRowVO>>(lastDayDataHolder.entrySet())) {
            if (entry.getValue().getLatestActivity() < inactiveBefore) {
                if (lastDayDataHolder.remove(entry.getKey()) != null) {
                    purgedKeys.add(entry.getKey());
                }
            }
        }
        purgedRows.addAndGet(purgedKeys.size());

        long evicted = evictedRows.getAndSet(0);
        long purged = purgedRows.getAndSet(0);
        long skipped = evictSkipped.getAndSet(0);

        Runtime runtime = Runtime.getRuntime();
        long usedKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
        long committedKb = runtime.totalMemory() / 1024;
        long maxKb = runtime.maxMemory() / 1024;

        logger.info("Metrics stores status: lastHour rows[{}], lastDay rows[{}], evicted[{}], purged[{}], evictSkipped[{}] since last status log; JVM heap used[{}]KB committed[{}]KB max[{}]KB",
                lastHourDataHolder.size(), lastDayDataHolder.size(), evicted, purged, skipped, usedKb, committedKb, maxKb);
        if (skipped > 0) {
            logger.warn("Metrics store row limit [{}] exceeded but no invisible rows to evict (skipped [{}])", maxRows, skipped);
        }
    }

    @Override
    public Collection<String> getMetricNames() {
        Collection<String> result = null;
        Collection<DataRowVO> uniqueMetrics = Collections2.filter(lastHourDataHolder.values(), new Predicate<DataRowVO>() {
            @Override
            public boolean apply(DataRowVO input) {
                return input.getMetricsName() != null && input.getMetricsName().equals(input.getDataSetName());
            }
        });
        if (uniqueMetrics != null && !uniqueMetrics.isEmpty()) {
            result = new ArrayList<>();
            for (DataRowVO dr : uniqueMetrics) {
                result.add(dr.getMetricsName());
            }
        }
        return result;
    }

    @Override
    public Collection<String> getDataSetsNames(final String metricName) {
        Collection<String> result = null;
        Collection<DataRowVO> uniqueMetrics = Collections2.filter(lastHourDataHolder.values(), new Predicate<DataRowVO>() {
            @Override
            public boolean apply(DataRowVO input) {
                return metricName.equals(input.getMetricsName());
            }
        });
        if (uniqueMetrics != null && !uniqueMetrics.isEmpty()) {
            result = new ArrayList<>();
            for (DataRowVO dr : uniqueMetrics) {
                result.add(dr.getDataSetName());
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Long>[] getCountsForLastHour(String metricName, String datasetName) {
        DataRowVO value = lastHourDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            return value.getCompleteCounts(System.currentTimeMillis());
        }
        return null;
    }

    @Override
    public DataPointVO<Long>[] getCountsForLastDay(String metricName, String datasetName) {
        DataRowVO value = lastDayDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            return value.getCompleteCounts(System.currentTimeMillis());
        }
        return null;
    }

    @Override
    public DataPointVO<Double>[] getMeansForLastHour(String metricName, String datasetName) {
        DataRowVO value = lastHourDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            return value.getCompleteMeans(System.currentTimeMillis());
        }
        return null;
    }

    @Override
    public DataPointVO<Double>[] getMeansForLastDay(String metricName, String datasetName) {
        DataRowVO value = lastDayDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            return value.getCompleteMeans(System.currentTimeMillis());
        }
        return null;
    }

    @Override
    public Date getLastActivityTime(String metricName, String datasetName) {

        long latestActivity = getLastActivityTimeMillis(metricName, datasetName);

        if (latestActivity == -1) {
            return null;
        }

        return new Date(latestActivity);
    }

    @Override
    public long getLastActivityTimeMillis(String metricName, String datasetName) {

        long result = -1;
        String key = MetricsDataUtils.getKey(metricName, datasetName);
        DataRowVO row = lastHourDataHolder.get(key);

        if (row == null) {
            row = lastDayDataHolder.get(key);
        }

        if (row != null) {
            long latestActivity = row.getLatestActivity();
            if (latestActivity > 0) {
                result = latestActivity;
            }
        }

        return result;
    }

    @Override
    public long getTotalCountOfLastHour(String metricName, String datasetName) {

        DataRowVO row = lastHourDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));

        long result = 0;
        if (row != null) {
            result = row.getTotalCount();
        }

        logger.debug("getTotalCountOfLastHour({}, {}) is [{}]", metricName, datasetName, result);
        return result;
    }

    @Override
    public long getTotalCountOfLastDay(String metricName, String datasetName) {

        DataRowVO row = lastDayDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));

        long result = 0;
        if (row != null) {
            result = row.getTotalCount();
        }

        logger.debug("getTotalCountOfLastDay({}, {}) is [{}]", metricName, datasetName, result);
        return result;
    }

    @Override
    public DatasetSummary getDatasetSummary(String metricName, String datasetName) {

        DatasetSummary summary = new DatasetSummary(metricName, datasetName);

        String key = MetricsDataUtils.getKey(metricName, datasetName);

        DataRowVO hourData = lastHourDataHolder.get(key);
        DataRowVO dayData = lastDayDataHolder.get(key);

        if (hourData != null) {
            summary.setHour(hourData.getSummary());
        }

        if (dayData != null) {
            summary.setDay(dayData.getSummary());
        }

        summary.setLastTime(getLastActivityTimeMillis(metricName, datasetName));

        return summary;
    }

    public void shutdown() {
        statusExecutor.shutdown();
    }

}
