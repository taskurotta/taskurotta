package ru.taskurotta.dropwizard.resources.console.metrics;

import com.google.common.base.Optional;
import ru.taskurotta.dropwizard.resources.console.BaseResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: dimadin
 * Date: 05.09.13 16:47
 */
@Path("/console/metrics/{action}")
public class MetricsResource extends BaseResource {

    public static final int TYPE_LOCAL_ACTOR_PROCESSING_TIME = 1;
    public static final int TYPE_CLUSTER_ACTOR_PROCESSING_TIME = 2;
    public static final int TYPE_ACTOR_DETAILED_STAT = 3;


    public static final String ACTION_METRICS_DATA = "data";
    public static final String ACTION_METRICS_METHOD_LIST = "methods";
    public static final String ACTION_METRICS_TYPE_LIST = "types";

    private Map<Integer, String> merticTypes;

    @GET
    public Response getMetrics(@PathParam("action") String action, @QueryParam("type") Optional<Integer> typeOpt,
                               @QueryParam("method") Optional<String> methodOpt, @QueryParam("dataset") Optional<String> dataSetOpt) {
        int type = typeOpt.or(-1);
        String method = methodOpt.or("");
        List<String> dataSetNames = extractDatasets(dataSetOpt.or(""));
        try {

            if(ACTION_METRICS_DATA.equals(action)) {
                return getMetricsDataResponse(type, method, dataSetNames);
            } else if(ACTION_METRICS_TYPE_LIST.equals(action)) {
                return getMetricsTypes();
            } else {
                throw new IllegalArgumentException("Unsupported action ["+action+"] for metrics getted");
            }

        } catch (Throwable e) {
            logger.error("Error getting metrics for type[" + type + "], method[" + method + "], actors[" + method + "]", e);
            return Response.serverError().build();
        }

    }

    private List<String> extractDatasets(String datasetStr) {
        logger.debug("Extracting dataset names from string[{}]", datasetStr);
        List<String> result = new ArrayList<>();
        if(datasetStr!=null && datasetStr.trim().length() > 0) {
            for(String dataset: datasetStr.split(",")) {
                if(dataset!=null) {
                    result.add(dataset.trim());
                }
            }
        }
        return result;
    }

    private Response getMetricsTypes() {
        List<MetricTypeVO> result = new ArrayList<>();
        if(merticTypes != null && !merticTypes.isEmpty()) {
            for (Integer key: merticTypes.keySet()) {
                MetricTypeVO item = new MetricTypeVO();
                item.setName(merticTypes.get(key));
                item.setType(key);
                result.add(item);
            }
        }
        logger.debug("Metrics types getted are [{}]", result);
        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }

    private Response getMetricsDataResponse(int type, String method, List<String> datasetNames) {
        int i = 0;
        if (TYPE_LOCAL_ACTOR_PROCESSING_TIME == type) {//dataset name == actorId
            List<DatasetVO> result = new ArrayList<>();
            for(String dataSetName: datasetNames) {
                result.add(getRandomDataset("X:t,sec; Y:&#916;t,ms  " + dataSetName, i++));
            }

            logger.debug("Metrics data response getted for type[{}], method[{}], datasetNames[{}] is [{}]", type, method, datasetNames, result);

            return Response.ok(result, MediaType.APPLICATION_JSON).build();

        } else if (TYPE_CLUSTER_ACTOR_PROCESSING_TIME == type) {//dataset name == actorId
            List<DatasetVO> result = new ArrayList<>();
            for(String dataSetName: datasetNames) {
                result.add(getRandomDataset("X:t,sec; Y:#916;t,ms  " + dataSetName, i++));
            }

            logger.debug("Metrics data response getted for type[{}], method[{}], datasetNames[{}] is [{}]", type, method, datasetNames, result);

            return Response.ok(result, MediaType.APPLICATION_JSON).build();

        } else if (TYPE_ACTOR_DETAILED_STAT == type) {//dataset == actor specific
            List<DatasetVO> result = new ArrayList<>();
            for(String dataSetName: datasetNames) {
                result.add(getRandomDataset(dataSetName, i++));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Metrics data response getted for type[{}], method[{}], datasetNames[{}] is [{}]", type, method, datasetNames, result);
            }

            return Response.ok(result, MediaType.APPLICATION_JSON).build();

        } else {

            if (logger.isDebugEnabled()) {
                logger.debug("Metrics type[{}] undefined, returning empty response", type);
            }

            return Response.ok(new ArrayList<>(), MediaType.APPLICATION_JSON).build();
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

    public void setMerticTypes(Map<Integer, String> merticTypes) {
        this.merticTypes = merticTypes;
    }
}
