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

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds metrics data collected via backend services methods invocations. Stores and exposes data.
 * Date: 19.09.13 10:30
 */
public class MetricsDataHandler implements DataListener, MetricsMethodDataRetriever, TimeConstants {
    private static MetricsDataHandler singleton;

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataHandler.class);
    private Map<String, DataRowVO> lastHourDataHolder = new ConcurrentHashMap<>();
    private Map<String, DataRowVO> lastDayDataHolder = new ConcurrentHashMap<>();

    public static MetricsDataHandler getInstance() {
        return singleton;
    }

    @PostConstruct
    public void init() {
        singleton = this;
    }

    @Override
    //triggers approximately once per second
    public void handle(String metricName, String datasetName, long count, double mean, long measurementTime) {
        String holderKey = MetricsDataUtils.getKey(metricName, datasetName);

        DataRowVO dataRow = lastHourDataHolder.get(holderKey);
        if (dataRow == null) {
            synchronized (lastHourDataHolder) {
                dataRow = lastHourDataHolder.get(holderKey);
                if (dataRow == null) {
                    dataRow = new DataRowVO(SECONDS_IN_HOUR, metricName, datasetName);
                    lastHourDataHolder.put(holderKey, dataRow);
                }
            }
        }

        int position = dataRow.populate(count, mean, measurementTime);
        if (position != 0 && position % SECONDS_IN_MINUTE == 0) {//new minute started
            handleMinute(metricName, datasetName, dataRow.getTotalCount(position - SECONDS_IN_MINUTE, position), dataRow.getAverageMean(), measurementTime);
        }
        logger.trace("Handled data for second [{}], metric[{}], dataset[{}], count[{}], mean[{}], measurementTime[{}]", position, metricName, datasetName, count, mean, measurementTime);
    }

    public void handleMinute(String metricName, String datasetName, long count, double mean, long measurementTime) {
        String holderKey = MetricsDataUtils.getKey(metricName, datasetName);

        DataRowVO dataRow = lastDayDataHolder.get(holderKey);
        if (dataRow == null) {
            synchronized (lastDayDataHolder) {
                dataRow = lastDayDataHolder.get(holderKey);
                if (dataRow == null) {
                    dataRow = new DataRowVO(MINUTES_IN_24_HOURS, metricName, datasetName);
                    lastDayDataHolder.put(holderKey, dataRow);
                }
            }
        }

        int position = dataRow.populate(count, mean, measurementTime);
        logger.trace("Handled data for minute [{}], metric[{}], dataset[{}], count[{}], mean[{}], measurementTime[{}]", position, metricName, datasetName, count, mean, measurementTime);
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
        DataPointVO<Long>[] result = null;
        DataRowVO row = lastHourDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (row != null) {
            int size = row.getDsCounts().length();
            result = new DataPointVO[size];
            for (int i = 0; i < size; i++) {
                result[i] = row.getDsCounts().get(i);
            }
        }
        logger.debug("getCountsForLastHour({}, {}) is [{}]", metricName, datasetName, result);
        return result;
    }

    @Override
    public DataPointVO<Long>[] getCountsForLastDay(String metricName, String datasetName) {
        DataPointVO<Long>[] result = null;
        DataRowVO row = lastDayDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (row != null) {
            int size = row.getDsCounts().length();
            result = new DataPointVO[size];
            for (int i = 0; i < size; i++) {
                result[i] = row.getDsCounts().get(i);
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Double>[] getMeansForLastHour(String metricName, String datasetName) {
        DataPointVO<Double>[] result = null;
        DataRowVO row = lastHourDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (row != null) {
            int size = row.getDsMean().length();
            result = new DataPointVO[size];
            for (int i = 0; i < size; i++) {
                result[i] = row.getDsMean().get(i);
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Double>[] getMeansForLastDay(String metricName, String datasetName) {
        DataPointVO<Double>[] result = null;
        DataRowVO row = lastDayDataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (row != null) {
            int size = row.getDsMean().length();
            result = new DataPointVO[size];
            for (int i = 0; i < size; i++) {
                result[i] = row.getDsMean().get(i);
            }
        }
        return result;
    }

    @Override
    public Date getLastActivityTime(String metricName, String datasetName) {

        Date result = null;
        String key = MetricsDataUtils.getKey(metricName, datasetName);
        DataRowVO row = lastHourDataHolder.get(key);

        if (row == null) {
            row = lastDayDataHolder.get(key);
        }

        if (row != null) {
            long latestActivity = row.getLatestActivity();
            if (latestActivity > 0) {
                result = new Date(latestActivity);
            }
        }

        return result;
    }

}
