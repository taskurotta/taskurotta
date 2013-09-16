package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.console.retriever.MetricsDataRetriever;
import ru.taskurotta.backend.statistics.DataPointVO;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides metrics data for console resource
 * User: dimadin
 * Date: 09.09.13 16:27
 */
public class MetricsDataHandler implements MetricsConstants {

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataHandler.class);

    private MetricsDataRetriever dataRetriever;

    public Response getDataResponse(String metricName, List<String> dataSetNames, String scope, String dataType, String period, boolean filterZeroValues) {
        int i = 0;
        List<DatasetVO> result = new ArrayList<>();
        for (String dataSetName : dataSetNames) {
            result.add(getDataset(metricName, dataSetName, dataType, period, filterZeroValues));
        }
        logger.debug("List<DatasetVO> getted for metricName[{}], dataType[{}], period[{}], dataSetNames[{}] is [{}]", metricName, dataType, period, dataSetNames, result);
        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }


    private DatasetVO getDataset(String metricName, String dataSetName, String dataType, String period, boolean filterZeroValues) {
        DatasetVO ds = new DatasetVO();
        ds.setLabel(constructLabel(dataSetName, dataType, period));

        if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            DataPointVO<Long>[] rawData = dataRetriever.getCountsForLastDay(metricName, dataSetName);
            ds.setData(convertToLongsDataRow(rawData, filterZeroValues));
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            DataPointVO<Long>[] rawData = dataRetriever.getCountsForLastHour(metricName, dataSetName);
            ds.setData(convertToLongsDataRow(rawData, filterZeroValues));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            DataPointVO<Double>[] rawData = dataRetriever.getMeansForLastDay(metricName, dataSetName);
            ds.setData(convertToDoublesDataRow(rawData, filterZeroValues));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            DataPointVO<Double>[] rawData = dataRetriever.getMeansForLastHour(metricName, dataSetName);
            ds.setData(convertToDoublesDataRow(rawData, filterZeroValues));
        } else {
           throw new IllegalArgumentException("Unsupported dataType["+dataType+"] and period["+period+"] combination");
        }

        return ds;
    }

    private List<double[]> convertToLongsDataRow(DataPointVO<Long>[] target, boolean filterZeroes) {
        List<double[]> result = new ArrayList<>();
        if(target!=null && target.length> 0) {
            for(int i = 0; i<target.length; i++) {
                //double timeMark = Double.valueOf(target[i].getTime());
                double value = Double.valueOf(target[i].getValue());
                double[] item = {i, value};
                if(!(filterZeroes && value==0)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private List<double[]> convertToDoublesDataRow(DataPointVO<Double>[] target, boolean filterZeroes) {
        List<double[]> result = new ArrayList<>();
        if(target!=null && target.length> 0) {
            for(int i = 0; i<target.length; i++) {
                //double timeMark = Double.valueOf(target[i].getTime());
                double value = target[i].getValue();
                double[] item = {i, value};
                if(!(filterZeroes && value==0)) {
                    result.add(item);
                }
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

//    private DatasetVO getRandomDataset(String dataSetName, int id) {
//        DatasetVO ds = new DatasetVO();
//        ds.setId(id);
//        ds.setLabel(dataSetName);
//        ds.setData(getRandomDataRow());
//        ds.setClickable(true);
//        ds.setHoverable(true);
//
//        return ds;
//    }

//    private List<double[]> getRandomDataRow() {
//        List<double[]> result = new ArrayList<>();
//        for (int i = 0; i < 50; i++) {
//            double[] item = {i, Math.random()*100};
//            result.add(item);
//        }
//        return result;
//    }

    @Required
    public void setDataRetriever(MetricsDataRetriever dataRetriever) {
        this.dataRetriever = dataRetriever;
    }

}
