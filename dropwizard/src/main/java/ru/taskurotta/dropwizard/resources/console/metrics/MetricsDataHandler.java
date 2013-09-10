package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public Response getMetricsDataResponse(String metric, String scope, String dataType, String period, List<String> datasetNames) {
        int i = 0;
        List<DatasetVO> result = new ArrayList<>();
        for(String dataSetName: datasetNames) {
            result.add(getRandomDataset("X:t,sec; Y:&#916;t,ms  " + dataSetName, i++));
        }

        logger.debug("Metrics data response getted for metric[{}], scope[{}], dataType[{}], period[{}], datasetNames[{}] is [{}]", metric, scope, dataType, period, datasetNames, result);

        return Response.ok(result, MediaType.APPLICATION_JSON).build();

//        if (TYPE_LOCAL_ACTOR_PROCESSING_TIME == type) {//dataset name == actorId
//            List<DatasetVO> result = new ArrayList<>();
//            for(String dataSetName: datasetNames) {
//                result.add(getRandomDataset("X:t,sec; Y:&#916;t,ms  " + dataSetName, i++));
//            }
//
//            logger.debug("Metrics data response getted for type[{}], method[{}], datasetNames[{}] is [{}]", type, method, datasetNames, result);
//
//            return Response.ok(result, MediaType.APPLICATION_JSON).build();
//
//        } else if (TYPE_CLUSTER_ACTOR_PROCESSING_TIME == type) {//dataset name == actorId
//            List<DatasetVO> result = new ArrayList<>();
//            for(String dataSetName: datasetNames) {
//                result.add(getRandomDataset("X:t,sec; Y:#916;t,ms  " + dataSetName, i++));
//            }
//
//            logger.debug("Metrics data response getted for type[{}], method[{}], datasetNames[{}] is [{}]", type, method, datasetNames, result);
//
//            return Response.ok(result, MediaType.APPLICATION_JSON).build();
//
//        } else if (TYPE_ACTOR_DETAILED_STAT == type) {//dataset == actor specific
//            List<DatasetVO> result = new ArrayList<>();
//            for(String dataSetName: datasetNames) {
//                result.add(getRandomDataset(dataSetName, i++));
//            }
//
//            if (logger.isDebugEnabled()) {
//                logger.debug("Metrics data response getted for type[{}], method[{}], datasetNames[{}] is [{}]", type, method, datasetNames, result);
//            }
//
//            return Response.ok(result, MediaType.APPLICATION_JSON).build();
//
//        } else {
//
//            if (logger.isDebugEnabled()) {
//                logger.debug("Metrics type[{}] undefined, returning empty response", type);
//            }
//
//            return Response.ok(new ArrayList<>(), MediaType.APPLICATION_JSON).build();
//        }

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


}
