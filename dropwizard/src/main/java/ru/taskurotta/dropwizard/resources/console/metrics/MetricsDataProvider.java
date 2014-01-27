package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConsoleUtils;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConstants;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO;
import ru.taskurotta.service.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.service.console.retriever.metrics.MetricsNumberDataRetriever;
import ru.taskurotta.service.metrics.MetricsDataUtils;
import ru.taskurotta.service.metrics.TimeConstants;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides metrics data for console resource
 * User: dimadin
 * Date: 09.09.13 16:27
 */
public class MetricsDataProvider implements MetricsConstants, TimeConstants {

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataProvider.class);

    private MetricsMethodDataRetriever methodDataRetriever;
    private MetricsNumberDataRetriever numberDataRetriever;
    private int methodMetricsPeriodSeconds = 0;
    private Map<String, Integer> periodicMetricPeriods = null;

    public List<DatasetVO> getDataResponse(String metricName, List<String> dataSetNames, String scope, String dataType, String period) {
        List<DatasetVO> result = new ArrayList<>();
        for (String dataSetName : dataSetNames) {
            DatasetVO ds = getDataset(metricName, dataSetName, dataType, period);
            result.add(ds);
        }
        logger.debug("Datasets extracted for metric[{}], datasets[{}], scope[{}], dataType[{}], period[{}] are[{}]", metricName, dataSetNames, scope, dataType, period, result);
        return result;
    }

    private DatasetVO getDataset(String metricName, String dataSetName, String dataType, String period) {
        if (hasMethodMetric(metricName)) {
            return getMethodDataset(metricName, dataSetName, dataType, period);

        } else if(hasNumberMetric(metricName)) {
            return getNumberDataset(metricName, dataSetName, dataType, period);

        } else {
            throw new IllegalArgumentException("Unsupported metricName["+metricName+"]");
        }
    }

    protected boolean hasMethodMetric(String metric) {
        Collection <String> names = methodDataRetriever.getMetricNames();
        return names != null && names.contains(metric);
    }

    protected boolean hasNumberMetric(String metric) {
        Collection <String> names = numberDataRetriever.getNumberMetricNames();
        return names != null && names.contains(metric);
    }

    private DatasetVO getNumberDataset(String metricName, String dataSetName, String dataType, String period) {
        if (!OPT_DATATYPE_SIZE.equals(dataType)) {
            throw new IllegalArgumentException("Unsupported dataType ["+dataType+"] for metric ["+metricName+"]");
        }

        DatasetVO ds = new DatasetVO();
        ds.setLabel(dataSetName);

        DataPointVO<Number>[] rawData = numberDataRetriever.getData(metricName, dataSetName);
        MetricsDataUtils.sortDataSet(rawData);


        int periodSeconds = periodicMetricPeriods.containsKey(metricName)? periodicMetricPeriods.get(metricName): 5;//5 sec is default for now
        DataPointVO <Number>[] timeLimitedSubset = getSubset(rawData, getSubsetSizeForPeriod(rawData.length, periodSeconds, period));
        List<Number[]> dataPoints = null;
        dataPoints = MetricsDataUtils.convertToTimedDataRow(timeLimitedSubset);
        ds.setyFormatter("memory");
        ds.setyLabel("Memory");
        ds.setxFormatter("time");
        ds.setxLabel("Time");


//        if (MetricName.MEMORY.getValue().equals(metricName)) {
//            dataPoints = MetricsDataUtils.convertToTimedDataRow(timeLimitedSubset);
//            ds.setyFormatter("memory");
//            ds.setxFormatter("time");
//            ds.setxLabel("Time");
//            ds.setyLabel("Memory");
//
//
//        } else {
//            float multiplier = Float.valueOf(numberMetricsPeriodSeconds) * getTimestepMultiplier(period);
//            dataPoints = MetricsDataUtils.convertToDataRow(timeLimitedSubset, true, multiplier);
//            ds.setxLabel(MetricsConsoleUtils.getXLabel(dataType, period));
//            ds.setyLabel(MetricsConsoleUtils.getYLabel(dataType, period));
//            if (logger.isDebugEnabled()) {
//                logger.debug("DataPoint for period[{}], multiplier[{}] are [{}]", period, multiplier, getDatapointAsString(dataPoints));
//            }
//        }

        ds.setData(dataPoints);

        logger.debug("Datapoint subset size is[{}], original size is [{}], periodSeconds [{}], period[{}]", dataPoints!=null? dataPoints.size(): null, rawData!=null? rawData.length: null, periodSeconds, period);

        return ds;
    }

    private static String getDatapointAsString(List<Number[]> dataPoints) {
        StringBuilder result = new StringBuilder();
        if (dataPoints!=null) {
            for (Number[] point : dataPoints) {
                if (result.length()>0) {
                    result.append(", ");
                }
                result.append("[").append(point[0]).append("("+point[0].getClass().getName()+")").append(", ").append(point[1]).append("]");
            }
        }
        return result.toString();
    }

    private static float getTimestepMultiplier(String period) {
        float result = 1;
        if (period!=null && period.toUpperCase().contains("HOUR")) {//minutes in timeline
            result = 1f/60f;
        }
        return result;
    }

    private static int getSubsetSizeForPeriod (int totalSize, int timeStep, String period) {
        int result = totalSize;

        if (period != null) {
            period = period.toUpperCase();
            if (period.contains("MINUTE")) {
                result = SECONDS_IN_MINUTE * getNumberFromString(period, result)/timeStep;
            } else if (period.contains("SECOND")) {
                result = getNumberFromString(period, result)/timeStep;
            } else if (period.contains("HOUR")) {
                result = SECONDS_IN_HOUR * getNumberFromString(period, result)/timeStep;
            }
        }

        return result;
    }

    private static int getNumberFromString(String period, int defVal) {
        int result = defVal;
        if (period != null) {
            String timeString = period.replaceAll("\\D", "").trim();
            try {
                result = Integer.parseInt(timeString);
            } catch (Exception e) {
                logger.error("Cannot parse minutes from string["+period+"], fallback to defVal["+defVal+"]", e);
            }
        }

        return result;
    }

    private DataPointVO<Number>[] getSubset(DataPointVO<Number>[] rawData, int size) {
        DataPointVO<Number>[] result = null;
        if (rawData != null) {
            if (size >= rawData.length) {
                result = rawData;

            } else {
                result = new DataPointVO[size];
                int offset = rawData.length - size;
                for (int i = 0; i < size; i++) {
                    result[i] = rawData[offset+i];
                }
            }
        }
        return result;
    }

    private DatasetVO getMethodDataset(String metricName, String dataSetName, String dataType, String period) {
        DatasetVO ds = new DatasetVO();
        ds.setLabel(dataSetName);

        //ds.setxLabel(MetricsConsoleUtils.getXLabel(dataType, period));
        ds.setxFormatter("time");
        ds.setxLabel("Time");

        ds.setyLabel(MetricsConsoleUtils.getYLabel(dataType, period));

        boolean useTimeline = true;
        DataPointVO<? extends Number>[] rawData = null;

        if (OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            rawData = methodDataRetriever.getCountsForLastDay(metricName, dataSetName);
        } else if (OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            rawData = methodDataRetriever.getCountsForLastHour(metricName, dataSetName);
        } else if (OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            rawData = methodDataRetriever.getMeansForLastDay(metricName, dataSetName);
        } else if (OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            rawData = methodDataRetriever.getMeansForLastHour(metricName, dataSetName);
        } else {
            throw new IllegalArgumentException("Unsupported dataType["+dataType+"] and period["+period+"] combination");
        }

        MetricsDataUtils.sortDataSet(rawData);
        ds.setData(MetricsDataUtils.convertToTimedDataRow(rawData));
//        ds.setData(MetricsDataUtils.convertToDataRow(rawData, useTimeline, methodMetricsPeriodSeconds));

        return ds;
    }

    @Required
    public void setMethodDataRetriever(MetricsMethodDataRetriever methodDataRetriever) {
        this.methodDataRetriever = methodDataRetriever;
    }
    @Required
    public void setNumberDataRetriever(MetricsNumberDataRetriever numberDataRetriever) {
        this.numberDataRetriever = numberDataRetriever;
    }

    @Required
    public void setMethodMetricsPeriodSeconds(int methodMetricsPeriodSeconds) {
        this.methodMetricsPeriodSeconds = methodMetricsPeriodSeconds;
    }

    @Required
    public void setPeriodicMetricPeriods(Map<String, Integer> periodicMetricPeriods) {
        this.periodicMetricPeriods = periodicMetricPeriods;
    }
}
