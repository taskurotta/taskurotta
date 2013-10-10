package ru.taskurotta.backend.statistics;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.retriever.MetricsDataRetriever;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * User: dimadin
 * Date: 19.09.13 10:30
 */
public class MetricsDataHandler implements DataListener, MetricsDataRetriever {
    private static MetricsDataRetriever singleton;

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataHandler.class);

    private static final int MINUTES_IN_HOUR = 60;
    private static final int SECONDS_IN_HOUR = 3600;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MINUTES_IN_24_HOURS = 24 * MINUTES_IN_HOUR;

    private Map<String, DataRowVO> lastHourDataHolder = new ConcurrentHashMap<>();
    private Map<String, DataRowVO> lastDayDataHolder = new ConcurrentHashMap<>();

    public static MetricsDataRetriever getDataRetrieverInstance() {
        return singleton;
    }

    private void init() {
        singleton = this;
    }

    private static class DataRowVO {
        private final String metricName;
        private final String dataSetName;
        private int size = -1;

        private final AtomicInteger counter = new AtomicInteger(0);
        private AtomicReferenceArray<DataPointVO<Long>> dsCounts;
        private AtomicReferenceArray<DataPointVO<Double>> dsMean;

        private AtomicLong updated = new AtomicLong(-1);
        private AtomicLong lastActive = new AtomicLong(-1);


        public DataRowVO (int size, String metricName, String dataSetName) {
            this.metricName = metricName;
            this.dataSetName = dataSetName;
            this.size = size;

            this.dsCounts = new AtomicReferenceArray<DataPointVO<Long>>(size);
            this.dsMean = new AtomicReferenceArray<DataPointVO<Double>>(size);
        }

        //Return updated position
        public int populate(long count, double mean, long measureTime) {
            int position = getPosition();
            this.dsCounts.set(position, new DataPointVO<Long>(count, measureTime));
            this.dsMean.set(position, new DataPointVO<Double>(mean, measureTime));

            this.updated.set(new Date().getTime());
            if (count>0 && lastActive.get()<measureTime) {
                this.lastActive.set(measureTime);
            }
            return position;
        }

        private int getPosition() {
            counter.compareAndSet(this.size, 0);
            return counter.getAndIncrement();
        }

        public String getMetricsName() {
            return metricName;
        }

        public String getDataSetName(){
            return dataSetName;
        }

        public double getAverageMean() {
            double result = 0l;
            int resultCount = 0;

            for (int i = 0; i < this.size; i++) {
                if (dsMean.get(i) != null && dsMean.get(i).getValue()!=null && dsMean.get(i).getValue()>=0) {
                    result += dsMean.get(i).getValue();
                    resultCount++;
                }
            }
            if (resultCount>0){
                result = result/Double.valueOf(resultCount);
            }
            return result;
        }

        public long getTotalCount(int positionFrom, int positionTo) {
            long result = 0l;
            for (int i = positionFrom; i < positionTo; i++) {
                if (dsCounts.get(i) != null && dsCounts.get(i).getValue()!=null && dsCounts.get(i).getValue()>=0) {
                    result += dsCounts.get(i).getValue();
                }
            }
            return result;
        }

        public long getUpdated() {
            return this.updated.get();
        }

        public long getLatestActivity() {
            return this.lastActive.get();
        }

    }

    public static String getKey(String metricName, String datasetName) {
        return metricName + "#" + datasetName;
    }

    @Override
    //triggers approximately once per second
    public void handle(String metricName, String datasetName, long count, double mean, long measurementTime) {
        String holderKey = getKey(metricName, datasetName);

        if(!lastHourDataHolder.containsKey(holderKey)) {
            synchronized (lastHourDataHolder) {
                if(!lastHourDataHolder.containsKey(holderKey)) {
                    lastHourDataHolder.put(holderKey, new DataRowVO(SECONDS_IN_HOUR, metricName, datasetName));
                }
            }
        }

        DataRowVO dataRow = lastHourDataHolder.get(holderKey);
        int position = dataRow.populate(count, mean, measurementTime);
        if (position!=0 && position%SECONDS_IN_MINUTE == 0) {//new minute started
            handleMinute(metricName, datasetName, dataRow.getTotalCount(position-SECONDS_IN_MINUTE, position), dataRow.getAverageMean(), measurementTime);
        }
        logger.debug("Handled data for second [{}], metric[{}], dataset[{}], count[{}], mean[{}], measurementTime[{}]", position, metricName, datasetName, count, mean, measurementTime);
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

        int position = dataRow.populate(count, mean, measurementTime);
        logger.debug("Handled data for minute [{}], metric[{}], dataset[{}], count[{}], mean[{}], measurementTime[{}]", position, metricName, datasetName, count, mean, measurementTime);

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
            int size = row.dsCounts.length();
            result = new DataPointVO[size];
            for(int i = 0; i < size; i++) {
                result[i] = row.dsCounts.get(i);
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Long>[] getCountsForLastDay(String metricName, String datasetName) {
        DataPointVO<Long>[] result = null;
        DataRowVO row = lastDayDataHolder.get(getKey(metricName, datasetName));
        if(row != null) {
            int size = row.dsMean.length();
            result = new DataPointVO[size];
            for(int i = 0; i<size; i++) {
                result[i] = row.dsCounts.get(i);
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Double>[] getMeansForLastHour(String metricName, String datasetName) {
        DataPointVO<Double>[] result = null;
        DataRowVO row = lastHourDataHolder.get(getKey(metricName, datasetName));
        if(row != null) {
            int size = row.dsMean.length();
            result = new DataPointVO[size];
            for(int i = 0; i<size; i++) {
                result[i] = row.dsMean.get(i);
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Double>[] getMeansForLastDay(String metricName, String datasetName) {
        DataPointVO<Double>[] result = null;
        DataRowVO row = lastDayDataHolder.get(getKey(metricName, datasetName));
        if(row != null) {
            int size = row.dsMean.length();
            result = new DataPointVO[size];
            for(int i = 0; i<size; i++) {
                result[i] = row.dsMean.get(i);
            }
        }
        return result;
    }

    @Override
    public Date getLastActivityTime(String metricName, String datasetName) {
        Date result = null;
        String key = getKey(metricName, datasetName);
        DataRowVO row = lastHourDataHolder.get(key);

        if (row == null) {
            row = lastDayDataHolder.get(getKey(metricName, datasetName));
        }

        if (row != null) {
            long latestActivity = row.getLatestActivity();
            if (latestActivity>0) {
                result = new Date(latestActivity);
            }
        }

        return result;
    }

}
