package ru.taskurotta.service.metrics.model;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * User: dimadin
 * Date: 24.10.13 16:47
 */
public class DataRowVO extends BaseDataRowVO {

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
        double result = 0l;
        int resultCount = 0;

        for (int i = 0; i < this.size; i++) {
            if (dsMean.get(i) != null && dsMean.get(i).getValue() != null && dsMean.get(i).getValue() >= 0) {
                result += dsMean.get(i).getValue();
                resultCount++;
            }
        }

        if (resultCount > 0) {
            result = result / Double.valueOf(resultCount);
        }

        return result;
    }

    public long getTotalCount(int positionFrom, int positionTo) {
        long result = 0l;
        for (int i = positionFrom; i < positionTo; i++) {
            if (dsCounts.get(i) != null && dsCounts.get(i).getValue() != null && dsCounts.get(i).getValue() >= 0) {
                result += dsCounts.get(i).getValue();
            }
        }
        return result;
    }

    public AtomicReferenceArray<DataPointVO<Long>> getDsCounts() {
        return dsCounts;
    }

    public AtomicReferenceArray<DataPointVO<Double>> getDsMean() {
        return dsMean;
    }
}
