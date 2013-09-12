package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.statistics.ActorMetricsManager;
import ru.taskurotta.backend.statistics.GeneralMetricsManager;

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

    private ActorMetricsManager actorMetricsManager;
    private GeneralMetricsManager generalMetricsManager;

    public Response getActorMetricsDataResponse(boolean filterZeroValues, String metric, String scope, String dataType, String period, List<String> actorNames) {
        int i = 0;
        List<DatasetVO> result = new ArrayList<>();
        for(String dataSetName: actorNames) {
            result.add(getActorDataset(filterZeroValues, metric, dataType, period, dataSetName));
        }
        logger.debug("Actor metrics List<DatasetVO> getted for metricName[{}], dataType[{}], period[{}], actorNames[{}] is [{}]", metric, dataType, period, actorNames, result);
        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }

    public Response getGeneralMetricsDataResponse(boolean filterZeroValues, String metricName, String scope, String dataType, String period) {
        List<DatasetVO> result = new ArrayList<>();
        DatasetVO ds = new DatasetVO();
        ds.setLabel(constructLabel(metricName, dataType, period));

        if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            long[] rawData = generalMetricsManager.getDayCounts(metricName);
            ds.setData(convertToDataRow(rawData,filterZeroValues));
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            long[] rawData = generalMetricsManager.getHourCounts(metricName);
            ds.setData(convertToDataRow(rawData, filterZeroValues));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            double[] rawData = generalMetricsManager.getDayMeans(metricName);
            ds.setData(convertToDataRow(rawData, filterZeroValues));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            double[] rawData = generalMetricsManager.getHourMeans(metricName);
            ds.setData(convertToDataRow(rawData, filterZeroValues));
        } else {
            throw new IllegalArgumentException("Unsupported dataType["+dataType+"] and period["+period+"] combination");
        }

        result.add(ds);

        logger.debug("General metrics List<DatasetVO> getted for metricName[{}], dataType[{}], period[{}] is [{}]", metricName, dataType, period, result);

        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }

    private DatasetVO getActorDataset(boolean filterZeroValues, String metricName, String dataType, String period, String actorId) {
        DatasetVO ds = new DatasetVO();
        ds.setLabel(constructLabel(actorId, dataType, period));

        if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            long[] rawData = actorMetricsManager.getDayCounts(actorId, metricName);
            ds.setData(convertToDataRow(rawData, filterZeroValues));
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            long[] rawData = actorMetricsManager.getHourCounts(actorId, metricName);
            ds.setData(convertToDataRow(rawData, filterZeroValues));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            double[] rawData = actorMetricsManager.getDayMeans(actorId, metricName);
            ds.setData(convertToDataRow(rawData, filterZeroValues));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            double[] rawData = actorMetricsManager.getHourMeans(actorId, metricName);
            ds.setData(convertToDataRow(rawData, filterZeroValues));
        } else {
           throw new IllegalArgumentException("Unsupported dataType["+dataType+"] and period["+period+"] combination");
        }

        return ds;
    }

    private List<double[]> convertToDataRow(double[] target, boolean filterZeroes) {
        List<double[]> result = new ArrayList<>();
        if(target!=null && target.length> 0) {
            for(int i = 0; i<target.length; i++) {
                double[] item = {Double.valueOf(i), target[i]};
                if(!(filterZeroes && target[i]==0)) {
                    result.add(item);
                }

            }
        }
        return result;
    }

    private List<double[]> convertToDataRow(long[] target, boolean filterZeroes) {
        List<double[]> result = new ArrayList<>();
        if(target!=null && target.length> 0) {
            for(int i = 0; i<target.length; i++) {
                double[] item = {Double.valueOf(i), Double.valueOf(target[i])};
                if(!(filterZeroes && target[i]==0)) {
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

    private DatasetVO getRandomDataset(String dataSetName, int id) {
        DatasetVO ds = new DatasetVO();
        ds.setId(id);
        ds.setLabel(dataSetName);
        ds.setData(getRandomDataRow());
        ds.setClickable(true);
        ds.setHoverable(true);

        return ds;
    }

    private List<double[]> getRandomDataRow() {
        List<double[]> result = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            double[] item = {i, Math.random()*100};
            result.add(item);
        }
        return result;
    }

    @Required
    public void setActorMetricsManager(ActorMetricsManager actorMetricsManager) {
        this.actorMetricsManager = actorMetricsManager;
    }

    @Required
    public void setGeneralMetricsManager(GeneralMetricsManager generalMetricsManager) {
        this.generalMetricsManager = generalMetricsManager;
    }
}
