package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Performes some support operations (such as data filtering) on the metrics data sets.
 * User: dimadin
 * Date: 19.09.13 16:49
 */
public class MetricsDataFilter {

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataFilter.class);

    public static List<Number[]> getNonZeroValuesDataSet(List<Number[]> target) {
        List<Number[]> result = new ArrayList<>();
        if (target!=null && !target.isEmpty()) {
            for(Number[] item : target) {
                if(item[1]!=null && item[1].doubleValue() != 0d) {
                    result.add(item);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Remove zero values filter: before [{}] after [{}] points", target!=null? target.size(): null, result!=null? result.size(): null);
        }
        return result;
    }

    public static List<Number[]> getSmoothedDataSet(List<Number[]> target, int times) {
        List<Number[]> compressedData = new ArrayList<>();
        if (times>1 && target!=null && !target.isEmpty()) {
            for(int i = 0; i<target.size(); i++) {
                if (((i+1)%times) == 0) {
                    Number[] item = {target.get(i)[0], getAvarageValue(i, times-1, target)};
                    compressedData.add(item);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Smoothing data filter: before [{}] after [{}] points", target!=null? target.size(): null, compressedData!=null? compressedData.size(): null);
        }

        return compressedData;
    }

    private static Number getAvarageValue(int position, int siblingCount, List<Number[]> target) {
        int min = position-siblingCount;
        int max = position+siblingCount;
        min = min<0? 0: min;
        max = max>target.size()?target.size(): max;

        Class<? extends Number> targetValueClass = getFirstItemClass(target);
        Number value = target.get(position)[1];
        Number result = null;
        if (targetValueClass == null) {
            result = null;//No values
        } else if (Long.class.isAssignableFrom(targetValueClass)) {
            result = getAverageLongValue(target, min, max);

        } else if(Double.class.isAssignableFrom(targetValueClass)) {
            result = getAverageDoubleValue(target, min, max);

        } else {
            throw new IllegalArgumentException("Unsupported value class: " + (value!=null? value.getClass().getName(): null));
        }
        return result;
    }

    private static Class<? extends Number> getFirstItemClass(List<Number[]> target) {
        Class<? extends Number> result = null;
        if (target!=null && !target.isEmpty()) {
            for(Number[] item: target) {
                if (item!=null && item[1]!=null) {
                    result = item[1].getClass();
                    break;
                }
            }
        }
        return result;
    }

    private static Number getAverageLongValue(List<Number[]> target, int from, int to) {
        Long result = 0l;
        int count = 0;
        for (int i = from; i < to; i++) {
            if (target.get(i)!=null && target.get(i)[1]!=null) {
                result += target.get(i)[1].longValue();
            }
            count++;
        }
        return result/Long.valueOf(count);

    }

    private static Number getAverageDoubleValue(List<Number[]> target, int from, int to) {
        Double result = 0d;
        int count = 0;
        for(int i = from; i < to; i++) {
            if (target.get(i)!=null && target.get(i)[1]!=null) {
                result += target.get(i)[1].doubleValue();
            }
            count++;
        }

        return result/Double.valueOf(count);

    }

}
