package ru.taskurotta.service.metrics.model;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * User: dimadin
 * Date: 24.10.13 17:02
 */
public class NumberDataRowVO extends BaseDataRowVO {

    private AtomicReferenceArray<DataPointVO<Number>> data;

    public NumberDataRowVO(int size, String metricName, String dataSetName) {
        super(size, metricName, dataSetName);
        data = new AtomicReferenceArray<DataPointVO<Number>>(size);
    }

    //Return updated position
    public int populate(Number value, long measureTime) {
        int position = getPosition();
        DataPointVO<Number> positionValue = this.data.get(position);

        if (positionValue!=null) {
            positionValue.update(value, measureTime);
        } else {
            this.data.set(position, new DataPointVO<Number>(value, measureTime));
        }

        this.updated.set(new Date().getTime());
        if (lastActive.get() < measureTime) {
            this.lastActive.set(measureTime);
        }
        return position;
    }

    public DataPointVO<Number>[] getCurrentData() {
        DataPointVO<Number>[] result = new DataPointVO[size];
        for (int i = 0; i<size; i++) {
            result[i] = data.get(i);
        }
        return result;
    }

}
