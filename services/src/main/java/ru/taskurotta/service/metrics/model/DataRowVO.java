package ru.taskurotta.service.metrics.model;

import ru.taskurotta.service.metrics.model.BaseDataRowVO;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * User: dimadin
 * Date: 24.10.13 16:47
 */
public class DataRowVO extends BaseDataRowVO {

    private AtomicReferenceArray<DataPointVO<Long>> dsCounts;
    private AtomicReferenceArray<DataPointVO<Double>> dsMean;

    public DataRowVO (int size, String metricName, String dataSetName) {
        super(size, metricName, dataSetName);

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

    public AtomicReferenceArray<DataPointVO<Long>> getDsCounts() {
        return dsCounts;
    }

    public AtomicReferenceArray<DataPointVO<Double>> getDsMean() {
        return dsMean;
    }
}
