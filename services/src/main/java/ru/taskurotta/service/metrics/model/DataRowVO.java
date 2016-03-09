package ru.taskurotta.service.metrics.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * User: dimadin
 * Date: 24.10.13 16:47
 */
public class DataRowVO extends BaseDataRowVO {
    private static final Logger logger = LoggerFactory.getLogger(DataRowVO.class);

    private AtomicReferenceArray<DataPointVO<Long>> dsCounts;
    private AtomicReferenceArray<DataPointVO<Double>> dsMean;

    public DataRowVO(int size, String metricName, String dataSetName) {
        super(size, metricName, dataSetName);

        this.dsCounts = new AtomicReferenceArray<DataPointVO<Long>>(size);
        this.dsMean = new AtomicReferenceArray<DataPointVO<Double>>(size);
    }

    //Return updated position
    public int populate(long count, double mean, long measureTime) {

        int position = getPosition();
        DataPointVO countValue = this.dsCounts.get(position);
        if (countValue != null) {
            countValue.update(count, measureTime);

        } else {
            this.dsCounts.set(position, new DataPointVO<Long>(count, measureTime));
        }

        DataPointVO meanValue = this.dsMean.get(position);
        if (meanValue != null) {
            meanValue.update(mean, measureTime);

        } else {
            this.dsMean.set(position, new DataPointVO<Double>(mean, measureTime));
        }


        this.updated = System.currentTimeMillis();
        if (count > 0 && lastActive < measureTime) {
            this.lastActive = measureTime;
        }
        return position;
    }

    public double getAverageMean() {
        double result = 0L;
        int resultCount = 0;

        for (int i = 0; i < this.size; i++) {
            DataPointVO<Double> dataPoint = dsMean.get(i);
            if (dataPoint != null) {
                Double value = dsMean.get(i).getValue();
                if (value != null && value >= 0) {
                    result += value;
                    resultCount++;
                }
            }
        }

        if (resultCount > 0) {
            result = result / (double) resultCount;
        }

        return result;
    }

    public long getTotalCount(int positionFrom, int positionTo) {
        long result = 0L;
        for (int i = positionFrom; i < positionTo; i++) {
            DataPointVO<Long> dataPoint = dsCounts.get(i);
            if (dataPoint != null) {
                Long value = dataPoint.getValue();
                if (value != null && value >= 0) {
                    result += value;
                }
            }
        }
        return result;
    }

    public long getTotalCount() {
        return getTotalCount(0, this.size);
    }

    /**
     * Fastest way (one time scan) to get all summary data
     */
    public DataRowSummary getSummary() {
        DataRowSummary summary = new DataRowSummary();

        double meanSum = 0L;

        long count = 0L;
        long timeMin = Long.MAX_VALUE;
        long timeMax = 0L;

        for (int i = 0; i < this.size; i++) {

            DataPointVO<Double> meanPoint = dsMean.get(i);
            DataPointVO<Long> countPoint = dsCounts.get(i);

            if (meanPoint != null && countPoint != null) {

                Double meanValue = meanPoint.getValue();
                if (meanValue != null && meanValue >= 0) {
                    meanSum += meanValue * countPoint.getValue();

                    Long countValue = countPoint.getValue();
                    if (countValue != null && countValue >= 0) {
                        count += countValue;

                        long countPointTime = countPoint.getTime();
                        if (countPointTime > timeMax) {
                            timeMax = countPointTime;
                        }

                        if (countPointTime < timeMin) {
                            timeMin = countPointTime;
                        }
                    }
                }
            }
        }

        // reset temporary value
        if (timeMin == Long.MAX_VALUE) {
            timeMin = 0;
        }

        double mean = 0;
        if (count > 0) {
            mean = meanSum / (double) count;
        }

        summary.setMean(mean);
        summary.setCount(count);
        summary.setTimeMin(timeMin);
        summary.setTimeMax(timeMax);

        return summary;
    }

    public AtomicReferenceArray<DataPointVO<Long>> getDsCounts() {
        return dsCounts;
    }

    public AtomicReferenceArray<DataPointVO<Double>> getDsMean() {
        return dsMean;
    }
}
