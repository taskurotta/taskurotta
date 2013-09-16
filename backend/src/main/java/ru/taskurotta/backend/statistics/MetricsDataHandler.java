package ru.taskurotta.backend.statistics;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.AtomicDoubleArray;
import ru.taskurotta.backend.console.retriever.MetricsDataRetriever;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Handles data getted by metrics marks.
 * Stores data for last hour (with one second resolution) and for last 24 hours (with 1 minute resolution)
 * User: dimadin
 * Date: 13.09.13 12:24
 */
public class MetricsDataHandler implements DataListener, MetricsDataRetriever {

    private static final int SECONDS_IN_HOUR = 3600;
    private static final int MILLISECONDS_IN_SECONDS = 1000;
    private static final int MINUTES_IN_24_HOURS = 24 * 60;

    private Map<String, DataRowVO> lastHourDataHolder = new ConcurrentHashMap<>();
    private Map<String, DataRowVO> lastDayDataHolder = new ConcurrentHashMap<>();


    private static class DataRowVO {
        private final String metricName;
        private final String dataSetName;
        private final AtomicLongArray counts;
        private final AtomicDoubleArray means;
        private final AtomicLongArray timeLine;

        public DataRowVO (int size, String metricName, String dataSetName) {
            this.metricName = metricName;
            this.dataSetName = dataSetName;
            this.counts = new AtomicLongArray(size);
            this.means = new AtomicDoubleArray(size);
            this.timeLine = new AtomicLongArray(size);
        }

        public void populate(long count, double mean, long measureTime, int position) {
            this.counts.set(position, count);
            this.means.set(position, mean);
            this.timeLine.set(position, measureTime);
        }

        public String getMetricsName() {
            return metricName;
        }

        public String getDataSetName(){
            return dataSetName;
        }

        public long getAverageCount() {
            long summ = 0l;
            int size = counts.length();
            for(int i = 0; i < size; i++) {
                summ += counts.get(i);
            }
            return summ/(long)size;
        }

        public double getAverageMean() {
            double summ = 0l;
            int size = means.length();
            for(int i = 0; i < size; i++) {
                summ += means.get(i);
            }
            return summ/(double)size;
        }

    }

    public int getSecondPositionInLastHour(long time) {
        return (int) ((time / MILLISECONDS_IN_SECONDS) % SECONDS_IN_HOUR);
    }

    public static String getKey(String metricName, String datasetName) {
        return metricName + "#" + datasetName;
    }

    @Override
    //triggers approximately once per second
    public void handle(String metricName, String datasetName, long count, double mean, long measurementTime) {
        String holderKey = getKey(metricName, datasetName);

        DataRowVO dataRow = lastHourDataHolder.get(holderKey);
        if(dataRow == null) {
            synchronized (lastHourDataHolder) {
                if(!lastHourDataHolder.containsKey(holderKey)) {
                    lastHourDataHolder.put(holderKey, new DataRowVO(SECONDS_IN_HOUR, metricName, datasetName));
                }
            }
            dataRow = lastHourDataHolder.get(holderKey);
        }

        int position = getSecondPositionInLastHour(measurementTime);
        dataRow.populate(count, mean, measurementTime, position);
        if(position != 0 && (position % 59 == 0)) {//last second in minute
            handleMinute(metricName, datasetName, dataRow.getAverageCount(), dataRow.getAverageMean(), measurementTime);
        }

    }

    public void handleMinute(String metricName, String datasetName, long count, double mean, long measurementTime) {
        String holderKey = getKey(metricName, datasetName);

        DataRowVO dataRow = lastDayDataHolder.get(holderKey);
        if(dataRow == null) {
            synchronized (lastDayDataHolder) {
                if(!lastDayDataHolder.containsKey(holderKey)) {
                    lastDayDataHolder.put(holderKey, new DataRowVO(MINUTES_IN_24_HOURS, metricName, datasetName));
                }
            }
            dataRow = lastDayDataHolder.get(holderKey);
        }

        int position = getMinutePositionInLast24Hour(measurementTime);
        dataRow.populate(count, mean, measurementTime, position);

    }

    private static int getMinutePositionInLast24Hour(long measurementTime) {
        return (int) ((measurementTime / MILLISECONDS_IN_SECONDS / 60) % MINUTES_IN_24_HOURS);
    }

    @Override
    public Collection<String> getMetricNames() {
        Collection<String> result = null;
        Collection<DataRowVO> uniqueMetrics = Collections2.filter(lastHourDataHolder.values(), new Predicate<DataRowVO>() {
            @Override
            public boolean apply(DataRowVO input) {
                return input.getMetricsName()!=null && input.getMetricsName().equals(input.getDataSetName());
            }
        });
        if(uniqueMetrics!=null && !uniqueMetrics.isEmpty()) {
            result = new ArrayList<>();
            for(DataRowVO dr: uniqueMetrics) {
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
        if(uniqueMetrics!=null && !uniqueMetrics.isEmpty()) {
            result = new ArrayList<>();
            for(DataRowVO dr: uniqueMetrics) {
                result.add(dr.getDataSetName());
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Long>[] getCountsForLastHour(String metricName, String datasetName) {
        DataPointVO<Long>[] result = null;
        DataRowVO row = lastHourDataHolder.get(getKey(metricName, datasetName));
        if(row != null) {
            int size = row.counts.length();
            result = new DataPointVO[size];
            for(int i = 0; i<size; i++) {
                DataPointVO<Long> point = new DataPointVO<>();
                point.setTime(row.timeLine.get(i));
                point.setValue(row.counts.get(i));
                result[i] = point;
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Long>[] getCountsForLastDay(String metricName, String datasetName) {
        DataPointVO<Long>[] result = null;
        DataRowVO row = lastDayDataHolder.get(getKey(metricName, datasetName));
        if(row != null) {
            int size = row.counts.length();
            result = new DataPointVO[size];
            for(int i = 0; i<size; i++) {
                DataPointVO<Long> point = new DataPointVO<>();
                point.setTime(row.timeLine.get(i));
                point.setValue(row.counts.get(i));
                result[i] = point;
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Double>[] getMeansForLastHour(String metricName, String datasetName) {
        DataPointVO<Double>[] result = null;
        DataRowVO row = lastHourDataHolder.get(getKey(metricName, datasetName));
        if(row != null) {
            int size = row.counts.length();
            result = new DataPointVO[size];
            for(int i = 0; i<size; i++) {
                DataPointVO<Double> point = new DataPointVO<>();
                point.setTime(row.timeLine.get(i));
                point.setValue(row.means.get(i));
                result[i] = point;
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Double>[] getMeansForLastDay(String metricName, String datasetName) {
        DataPointVO<Double>[] result = null;
        DataRowVO row = lastDayDataHolder.get(getKey(metricName, datasetName));
        if(row != null) {
            int size = row.counts.length();
            result = new DataPointVO[size];
            for(int i = 0; i<size; i++) {
                DataPointVO<Double> point = new DataPointVO<>();
                point.setTime(row.timeLine.get(i));
                point.setValue(row.means.get(i));
                result[i] = point;
            }
        }
        return result;
    }


}
