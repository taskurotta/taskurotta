package ru.taskurotta.dropwizard.resources.console.metrics;

import com.google.common.base.Optional;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.statistics.metrics.MetricsDataUtils;
import ru.taskurotta.dropwizard.resources.console.BaseResource;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConsoleUtils;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConstants;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.AvailableOptionsVO;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * User: dimadin
 * Date: 05.09.13 16:47
 */
@Path("/console/metrics/{action}")
public class MetricsResource extends BaseResource implements MetricsConstants {

    private MetricsDataProvider metricsDataHandler;
    private MetricsOptionsProvider metricsOptionsHandler;

    @GET
    public Response getMetrics(@PathParam("action") String action, @QueryParam("type") Optional<String> typeOpt, @QueryParam("period") Optional<String> periodOpt,
                                    @QueryParam("scope") Optional<String> scopeOpt, @QueryParam("metric") Optional<String> metricOpt, @QueryParam("zeroes") Optional<Boolean> zeroesOpt,
                                    @QueryParam("dataset") Optional<String> dataSetOpt, @QueryParam("smooth") Optional<Integer> smoothOpt) {
        String dataType = typeOpt.or(OPT_UNDEFINED);
        String period = periodOpt.or(OPT_UNDEFINED);
        String scope = scopeOpt.or(OPT_UNDEFINED);
        String metricName = metricOpt.or(OPT_UNDEFINED);
        boolean zeroes = zeroesOpt.or(Boolean.TRUE);
        Integer smooth = smoothOpt.or(-1);

        List<String> dataSetNames = MetricsConsoleUtils.extractDatasets(dataSetOpt.or(""));
        try {

            if (ACTION_METRICS_DATA.equals(action)) {
                List<DatasetVO> dataSets = metricsDataHandler.getDataResponse(metricName, dataSetNames, scope, dataType, period);
                if (smooth > 0) {
                    for(DatasetVO ds: dataSets) {
                        ds.setData(MetricsDataUtils.getSmoothedDataSet(ds.getData(), smooth));
                    }
                }
                if (!zeroes) {
                    for(DatasetVO ds: dataSets) {
                        ds.setData(MetricsDataUtils.getNonZeroValuesDataSet(ds.getData()));
                    }
                }
                return Response.ok(dataSets, MediaType.APPLICATION_JSON).build();
            } else if(ACTION_METRICS_OPTIONS.equals(action)) {
                AvailableOptionsVO options = metricsOptionsHandler.getAvailableOptions();
                return Response.ok(options, MediaType.APPLICATION_JSON).build();

            } else {
                throw new IllegalArgumentException("Unsupported action ["+action+"] for metrics getted");
            }

        } catch (Throwable e) {
            logger.error("Error getting metrics for action[" + action + "], zeroes["+zeroes+"], metricName[" + metricName + "], dataType[" + dataType + "], period["+dataType+"], scope["+scope+"], dataSet["+dataSetNames+"]", e);
            return Response.serverError().build();
        }

    }

    @Required
    public void setMetricsDataHandler(MetricsDataProvider metricsDataHandler) {
        this.metricsDataHandler = metricsDataHandler;
    }

    @Required
    public void setMetricsOptionsHandler(MetricsOptionsProvider metricsOptionsHandler) {
        this.metricsOptionsHandler = metricsOptionsHandler;
    }

}
