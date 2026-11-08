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

    public DataRowVO(int size, long binMillis, String metricName, String dataSetName) {
        super(size, binMillis, metricName, dataSetName);

        this.dsCounts = new AtomicReferenceArray<DataPointVO<Long>>(size);
        this.dsMean = new AtomicReferenceArray<DataPointVO<Double>>(size);
    }

    /**
     * Запись отметки в бин по времени данных.
     * count бина накапливается (сумма за бин), mean бина — среднее, взвешенное по count
     * (merge: (mean_old * count_old + mean_new * count_new) / (count_old + count_new)).
     * Накопление — только внутри одного абсолютного слота; если слот переиспользован
     * (кольцо обошло полный оборот), данные прежнего оборота сбрасываются —
     * кольцо остаётся скользящим окном.
     * Idle-отметка (count=0) бин не пишет: неактивный ключ точками не заполняется,
     * пустые бины остаются «дырами» в графике.
     *
     * @return индекс бина, в который записана отметка; -1, если отметка не писалась (count=0)
     */
    public int populate(long count, double mean, long measureTime) {
        if (count <= 0) {
            return -1;
        }

        int position = binIndex(measureTime);
        long slot = measureTime / binMillis;

        long oldCount = 0L;
        DataPointVO<Long> countValue = dsCounts.get(position);
        // Кольцо — скользящее окно: слот переиспользуется каждый полный оборот.
        // Данные прежнего оборота (time из другого абсолютного слота) сбрасываются,
        // а не суммируются — иначе «всего за окно» накапливал бы все обороты.
        boolean sameSlot = countValue != null && countValue.getTime() / binMillis == slot;
        if (sameSlot && countValue.getValue() != null) {
            oldCount = countValue.getValue();
        }

        double oldMean = 0.0;
        DataPointVO<Double> meanValue = dsMean.get(position);
        if (sameSlot && meanValue != null && meanValue.getValue() != null) {
            oldMean = meanValue.getValue();
        }

        long totalCount = oldCount + count;
        double mergedMean = totalCount > 0 ? (oldMean * oldCount + mean * count) / totalCount : mean;

        if (countValue != null) {
            countValue.update(totalCount, measureTime);
        } else {
            dsCounts.set(position, new DataPointVO<Long>(totalCount, measureTime));
        }

        if (meanValue != null) {
            meanValue.update(mergedMean, measureTime);
        } else {
            dsMean.set(position, new DataPointVO<Double>(mergedMean, measureTime));
        }

        this.updated = System.currentTimeMillis();
        if (lastActive < measureTime) {
            this.lastActive = measureTime;
        }
        return position;
    }

    /**
     * «Всего за окно»: сумма всех бин кольца, включая недополненный текущий бин
     * (допуск: не более одного интервала агрегации).
     */
    public long getTotalCount() {
        long result = 0L;
        for (int i = 0; i < this.size; i++) {
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

    /**
     * Быстрый способ (один проход) получить все сводные данные по кольцу.
     * Средневзвешенное mean по count; учитывает все заполненные бины.
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

    /**
     * Снимок только полных бин (binStart + binSize &lt;= now): недополненный текущий бин
     * в отдачу графиков не попадает. Пустые бины — null (маркер «дыры»).
     */
    public DataPointVO<Long>[] getCompleteCounts(long now) {
        return snapshotCompleteBins(dsCounts, now);
    }

    public DataPointVO<Double>[] getCompleteMeans(long now) {
        return snapshotCompleteBins(dsMean, now);
    }

    @SuppressWarnings("unchecked")
    private <T extends Number> DataPointVO<T>[] snapshotCompleteBins(AtomicReferenceArray<DataPointVO<T>> array, long now) {
        int incompleteBin = binIndex(now);
        DataPointVO<T>[] result = new DataPointVO[size - 1];
        int k = 0;
        for (int i = 0; i < size; i++) {
            if (i == incompleteBin) {
                continue;
            }
            result[k++] = array.get(i);
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
