package ru.taskurotta.service.statistics.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.statistics.metrics.data.DataPointVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Performs some support operations (such as data filtering) on the metrics data sets.
 * Date: 19.09.13 16:49
 */
public class MetricsDataUtils {

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataUtils.class);

    public static String getKey(String metricName, String datasetName) {
        return metricName + "#" + datasetName;
    }

    public static List<Number[]> getNonZeroValuesDataSet(List<Number[]> target) {
        List<Number[]> result = new ArrayList<>();
        if (target!=null && !target.isEmpty()) {
            for(Number[] item : target) {
                if(isContainNotNegativeValue(item, true)) {
                    result.add(item);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Remove zero values filter: before [{}] after [{}] points", target!=null? target.size(): null, result!=null? result.size(): null);
        }
        return result;
    }


    public static boolean isContainNotNegativeValue(Number[] point, boolean nullAllowed) {
        boolean result = true;
        if (point!=null && point[0]!=null && point[1]!=null) {
            if (!isNotNegativeNumber(point[1], nullAllowed)) {//intrested only in value
                result = false;
            }
        } else{
            result = false;
        }
        return result;
    }


    public static boolean isNotNegativeNumber(Number target, boolean nullAllowed) {
        boolean result = false;
        if (target == null) {
           result = nullAllowed;

        } else {
            if (Long.class.isAssignableFrom(target.getClass())) {
                result = (Long)target >= 0l;
            } else if(Double.class.isAssignableFrom(target.getClass())) {
                result = (Double)target >= 0d;
            } else if(Integer.class.isAssignableFrom(target.getClass())) {
                result = (Integer)target >= 0;
            } else if(Float.class.isAssignableFrom(target.getClass())) {
                result = (Float)target >= 0f;
            }
        }
        return result;
    }

    public static List<Number[]> getSmoothedDataSet(List<Number[]> target, int times) {
        List<Number[]> compressedData = new ArrayList<>();
        if (times>1 && target!=null && !target.isEmpty()) {
            for(int i = 0; i<target.size(); i++) {
                if (((i+1)%times) == 0) {
                    Number[] item = {target.get(i)[0], getSiblingsAverage(i, times-1, target)};
                    compressedData.add(item);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Smoothing data filter: before [{}] after [{}] points", target!=null? target.size(): null, compressedData!=null? compressedData.size(): null);
        }

        return compressedData;
    }

    public static Number getSiblingsAverage(int position, int siblingCount, List<Number[]> target) {
        if (target == null) {
            return null;
        }
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

    public static Class<? extends Number> getFirstItemClass(List<Number[]> target) {
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

    public static Number getAverageLongValue(List<Number[]> target, int from, int to) {
        Long result = null;
        int count = 0;

        for (int i = from; i < to; i++) {
            if (isContainNotNegativeValue(target.get(i), false)) {
                if (result == null) {
                    result = 0l;
                }
                result += target.get(i)[1].longValue();
                count++;
            }
        }

        if(count>0) {
            result = result/Long.valueOf(count);
        }

        return result;

    }

    public static Number getAverageDoubleValue(List<Number[]> target, int from, int to) {
        Double result = null;
        int count = 0;
        for(int i = from; i < to; i++) {
            if (isContainNotNegativeValue(target.get(i), false)) {
                if (result == null) {
                    result = 0d;
                }
                result += target.get(i)[1].doubleValue();
                count++;
            }
        }

        if (count > 0){
            result = result/Double.valueOf(count);
        }

        return result;

    }

    public static List<Number[]> convertToDataRow(DataPointVO<? extends Number>[] target, boolean toTimeline, float timeStep) {
        List<Number[]> result = new ArrayList<>();
        if(target!=null && target.length> 0) {
            for (int i = 0; i < target.length; i++) {
                Number value = target[i]!=null? target[i].getValue(): null;

                Number[] item = {toTimeline? convertToTime(i, target[i]!=null? target[i].getTime(): 0, target.length, timeStep): i, value};
                result.add(item);
            }
        }
        return result;
    }

    public static List<Number[]> convertToTimedDataRow(DataPointVO<? extends Number>[] target) {
        List<Number[]> result = new ArrayList<>();
        if(target!=null && target.length> 0) {
            boolean hasNullGap = false;
            for (int i = 0; i < target.length; i++) {
                Number[] item = new Number[2];
                if (target[i] != null) {
                    item[0] = target[i].getTime();
                    item[1] = target[i].getValue();
                    result.add(item);
                    hasNullGap = false;
                } else if (!hasNullGap) {//to prevent adding multiple [null, null] entries. A single [null, null] should present indicating data gap
                    result.add(item);
                    hasNullGap = true;
                }
            }
        }
        return result;
    }

    public static Number convertToTime(int value, long time, int size, float timeStep) {
        return timeStep * Float.valueOf(value - size);
    }

    public static void sortDataSet(DataPointVO<? extends Number>[] target) {
        if(target!=null && target.length>0) {
            Arrays.sort(target, new Comparator<DataPointVO<? extends Number>>() {
                @Override
                public int compare(DataPointVO<? extends Number> o1, DataPointVO<? extends Number> o2) {
                    if (o1 == null && o2 == null) {
                        return 0;
                    } else if (o1 == null && o2 != null) {
                        return -1;
                    } else if (o1 != null && o2 == null) {
                        return 1;
                    } else {
                        if (o1.getTime() == o2.getTime()) {
                            return 0;
                        } else if (o1.getTime() < o2.getTime()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                }
            });
        }
    }


    public static long getTotalCounts(DataPointVO<Long>[] target) {
        long result = 0l;
        if (target!=null && target.length>0) {
            for (int i = 0; i<target.length; i++) {
                if (target[i]!=null && target[i].getValue()!=null && target[i].getValue()>=0) {
                    result += target[i].getValue();
                }
            }
        }
        return result;
    }

    public static double getMeanTime(DataPointVO<Double>[] target) {
        double result = 0d;
        int resultCount = 0;

        if (target!=null && target.length>0) {
            for (int i = 0; i<target.length; i++) {
                if (target[i]!=null && target[i].getValue()!=null && target[i].getValue()>=0) {
                    result += target[i].getValue();
                    resultCount++;
                }
            }
            if (resultCount>0) {
                result = result/Double.valueOf(resultCount);
            }
        }

        return result;
    }

    public static long sumUpDataPointsArray(DataPointVO<Long>[] target) {
        long result = 0l;

        if (target != null && target.length > 0) {
            result = 0l;
            for (DataPointVO<Long> dp: target) {
                if (dp!=null && dp.getValue()>0) {
                    result += dp.getValue();
                }
            }
        }

        return result;
    }



}
