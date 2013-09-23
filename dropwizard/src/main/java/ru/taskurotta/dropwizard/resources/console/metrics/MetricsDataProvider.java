package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.console.retriever.MetricsDataRetriever;
import ru.taskurotta.backend.statistics.DataPointVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Provides metrics data for console resource
 * User: dimadin
 * Date: 09.09.13 16:27
 */
public class MetricsDataProvider implements MetricsConstants {

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataProvider.class);

    private MetricsDataRetriever dataRetriever;

    public List<DatasetVO> getDataResponse(String metricName, List<String> dataSetNames, String scope, String dataType, String period) {
        List<DatasetVO> result = new ArrayList<>();
        for (String dataSetName : dataSetNames) {
            DatasetVO ds = getDataset(metricName, dataSetName, dataType, period);
            result.add(ds);
        }
        return result;
    }


    private DatasetVO getDataset(String metricName, String dataSetName, String dataType, String period) {
        DatasetVO ds = new DatasetVO();
        ds.setLabel(constructLabel(dataSetName, dataType, period));

        if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            DataPointVO<Long>[] rawData = dataRetriever.getCountsForLastDay(metricName, dataSetName);
            sortDataSet(rawData);
            ds.setData(convertToDataRow(rawData));
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            DataPointVO<Long>[] rawData = dataRetriever.getCountsForLastHour(metricName, dataSetName);
            sortDataSet(rawData);
            ds.setData(convertToDataRow(rawData));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            DataPointVO<Double>[] rawData = dataRetriever.getMeansForLastDay(metricName, dataSetName);
            sortDataSet(rawData);
            ds.setData(convertToDataRow(rawData));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            DataPointVO<Double>[] rawData = dataRetriever.getMeansForLastHour(metricName, dataSetName);
            sortDataSet(rawData);
            ds.setData(convertToDataRow(rawData));
        } else {
           throw new IllegalArgumentException("Unsupported dataType["+dataType+"] and period["+period+"] combination");
        }

        return ds;
    }

    private void sortDataSet(DataPointVO<? extends Number>[] target) {
        if(target!=null && target.length>0) {
            Arrays.sort(target, new Comparator<DataPointVO<? extends Number>>() {
                @Override
                public int compare(DataPointVO<? extends Number> o1, DataPointVO<? extends Number> o2) {
                    if (o1 == null && o2 == null) {
                        return 0;
                    } else if (o1 == null && o2 != null) {
                        return -1;
                    } else if(o1 != null && o2 == null) {
                        return 1;
                    } else {
                        if(o1.getTime() == o2.getTime()) {
                            return 0;
                        } else if(o1.getTime() < o2.getTime()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                }
            });
        }
    }

    private List<Number[]> convertToDataRow(DataPointVO<? extends Number>[] target) {
        List<Number[]> result = new ArrayList<>();
        if(target!=null && target.length> 0) {
            for (int i = 0; i<target.length; i++) {
                Number value = target[i]!=null? target[i].getValue(): null;
                Number[] item = {i, value};
                result.add(item);
            }
        }
        return result;
    }

    private String constructLabel(String label, String dataType, String period) {
        if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            return "X: time, min; Y: count. " + label;
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            return "X: time, s; Y: count. " + label;
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            return "X: time, min; Y: mean, ms. " + label;
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            return "X: time, s; Y: mean, ms. " + label;
        } else {
            return label;
        }
    }

    @Required
    public void setDataRetriever(MetricsDataRetriever dataRetriever) {
        this.dataRetriever = dataRetriever;
    }

}
