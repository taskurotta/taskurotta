package ru.taskurotta.service.metrics.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * User: dimadin
 * Date: 25.10.13 17:02
 */
public class NumberDataRowVO extends BaseDataRowVO {

    private static final Logger logger = LoggerFactory.getLogger(NumberDataRowVO.class);

    private AtomicReferenceArray<DataPointVO<Number>> data;

    public NumberDataRowVO(int size, long binMillis, String metricName, String dataSetName) {
        super(size, binMillis, metricName, dataSetName);
        data = new AtomicReferenceArray<DataPointVO<Number>>(size);
    }

    /**
     * Запись выборки в бин по времени данных: last-write-wins — в бине остаётся
     * последняя выборка за бин (период выборки короче бина).
     */
    //Return updated bin position
    public int populate(Number value, long measureTime) {

        int position = binIndex(measureTime);
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

    /**
     * «Последнее значение»: значение бина, в который попала последняя запись
     * (lastActive); если он пуст — свежайший заполненный бин до него.
     * Ничего не записано — null (null-guard).
     */
    public Number getLastValue() {
        if (lastActive <= 0) {
            return null;
        }

        int position = binIndex(lastActive);
        for (int i = 0; i < size; i++) {
            DataPointVO<Number> positionValue = data.get(position);
            if (positionValue != null && positionValue.getValue() != null) {
                return positionValue.getValue();
            }
            position = position - 1;
            if (position < 0) {
                position = size - 1;
            }
        }
        return null;
    }

    /**
     * Снимок только полных бин (binStart + binSize &lt;= now): недополненный текущий бин
     * в отдачу графиков не попадает. Пустые бины — null (маркер «дыры»).
     */
    @SuppressWarnings("unchecked")
    public DataPointVO<Number>[] getCompleteData(long now) {
        int incompleteBin = binIndex(now);
        DataPointVO<Number>[] result = new DataPointVO[size - 1];
        int k = 0;
        for (int i = 0; i < size; i++) {
            if (i == incompleteBin) {
                continue;
            }
            result[k++] = data.get(i);
        }
        return result;
    }

}
