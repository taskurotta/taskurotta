package ru.taskurotta.service.metrics.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * User: dimadin
 * Date: 24.10.13 17:02
 */
public class NumberDataRowVO extends BaseDataRowVO {

    private static final Logger logger = LoggerFactory.getLogger(NumberDataRowVO.class);

    private AtomicReferenceArray<DataPointVO<Number>> data;

    public NumberDataRowVO(int size, String metricName, String dataSetName) {
        super(size, metricName, dataSetName);
        data = new AtomicReferenceArray<DataPointVO<Number>>(size);
    }

    //Return updated position
    public int populate(Number value, long measureTime) {

        int position = getPosition();
        DataPointVO<Number> positionValue = data.get(position);

        if (positionValue != null) {
            positionValue.update(value, measureTime);
        } else {
            data.set(position, new DataPointVO<Number>(value, measureTime));
        }

        this.updated = System.currentTimeMillis();
        if (lastActive < measureTime) {
            lastActive = measureTime;
        }
        return position;
    }

    public Number getLastValue() {
        int position = getCurrentPositionOnly() - 1;
        if (position < 0) {
            position = size - 1;
        }

        // position can be incremented but value not populated yet - see populate() method
        DataPointVO<Number> positionValue = data.get(position);

        int previousPosition = position - 1;
        if (previousPosition < 0) {
            previousPosition = size - 1;
        }

        DataPointVO<Number> previousPositionValue = data.get(previousPosition);

        if (positionValue == null) {
            if (previousPositionValue != null) {
                // position value not populated yet
                return previousPositionValue.getValue();
            }

            return null;

        } else {
            if (positionValue.getTime() > previousPositionValue.getTime()) {
                return positionValue.getValue();
            }

            // position value has old data (not populated yet). use previous value
            return previousPositionValue.getValue();
        }

    }

    public DataPointVO<Number>[] getCurrentData() {
        DataPointVO<Number>[] result = new DataPointVO[size];
        for (int i = 0; i < size; i++) {
            result[i] = data.get(i);
        }
        return result;
    }

}
