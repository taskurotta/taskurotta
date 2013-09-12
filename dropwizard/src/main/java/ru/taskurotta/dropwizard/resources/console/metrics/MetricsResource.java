package ru.taskurotta.dropwizard.resources.console.metrics;

import com.google.common.base.Optional;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.dropwizard.resources.console.BaseResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * User: dimadin
 * Date: 05.09.13 16:47
 */
@Path("/console/metrics/{action}")
public class MetricsResource extends BaseResource implements MetricsConstants {

    private MetricsDataHandler metricsDataHandler;
    private MetricsOptionsHandler metricsOptionsHandler;

    @GET
    public Response getMetrics(@PathParam("action") String action, @QueryParam("type") Optional<String> typeOpt, @QueryParam("period") Optional<String> periodOpt,
                                    @QueryParam("scope") Optional<String> scopeOpt, @QueryParam("metric") Optional<String> metricOpt, @QueryParam("zeroes") Optional<Boolean> zeroesOpt,
                                    @QueryParam("dataset") Optional<String> dataSetOpt) {
        String dataType = typeOpt.or(OPT_UNDEFINED);
        String period = periodOpt.or(OPT_UNDEFINED);
        String scope = scopeOpt.or(OPT_UNDEFINED);
        String metric = metricOpt.or(OPT_UNDEFINED);
        boolean zeroes = zeroesOpt.or(Boolean.TRUE);

        List<String> dataSetNames = extractDatasets(dataSetOpt.or(""));
        try {

            if(ACTION_ACTOR_METRICS_DATA.equals(action)) {
                return metricsDataHandler.getActorMetricsDataResponse(!zeroes, metric, scope, dataType, period, dataSetNames);
            } else if(ACTION_GENERAL_METRICS_DATA.equals(action)) {
                return metricsDataHandler.getGeneralMetricsDataResponse(!zeroes, metric, scope, dataType, period);
            } else if(ACTION_METRICS_OPTIONS.equals(action)) {
                return metricsOptionsHandler.getMetricsTypes();

            } else {
                throw new IllegalArgumentException("Unsupported action ["+action+"] for metrics getted");
            }

        } catch (Throwable e) {
            logger.error("Error getting metrics for action[" + action + "], zeroes["+zeroes+"], metric[" + metric + "], dataType[" + dataType + "], period["+dataType+"], scope["+scope+"], dataSet["+dataSetNames+"]", e);
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


    @Required
    public void setMetricsDataHandler(MetricsDataHandler metricsDataHandler) {
        this.metricsDataHandler = metricsDataHandler;
    }

    @Required
    public void setMetricsOptionsHandler(MetricsOptionsHandler metricsOptionsHandler) {
        this.metricsOptionsHandler = metricsOptionsHandler;
    }

}
