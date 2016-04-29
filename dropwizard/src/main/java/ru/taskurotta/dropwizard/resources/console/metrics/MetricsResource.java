package ru.taskurotta.dropwizard.resources.console.metrics;

import com.google.common.base.Optional;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.dropwizard.resources.console.BaseResource;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConsoleUtils;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConstants;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.AvailableOptionsVO;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO;
import ru.taskurotta.service.metrics.MetricsDataUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.List;

/**
 * Date: 05.09.13 16:47
 */
@Path("/console/metrics")
public class MetricsResource extends BaseResource implements MetricsConstants {

    private MetricsDataProvider metricsDataHandler;
    private MetricsOptionsProvider metricsOptionsHandler;

    @GET
    @Path("/data")
    public List<DatasetVO> getMetricsData(@QueryParam("type") Optional<String> typeOpt, @QueryParam("period") Optional<String> periodOpt,
                                    @QueryParam("scope") Optional<String> scopeOpt, @QueryParam("metric") Optional<String> metricOpt, @QueryParam("zeroes") Optional<Boolean> zeroesOpt,
                                    @QueryParam("dataset") Optional<String> dataSetOpt, @QueryParam("smooth") Optional<Integer> smoothOpt) {

        String dataType = typeOpt.or(OPT_UNDEFINED);
        String period = periodOpt.or(OPT_UNDEFINED);
        String scope = scopeOpt.or(OPT_UNDEFINED);
        String metricName = metricOpt.or(OPT_UNDEFINED);
        boolean zeroes = zeroesOpt.or(Boolean.TRUE);
        Integer smooth = smoothOpt.or(-1);

        List<String> dataSetNames = MetricsConsoleUtils.extractDatasets(dataSetOpt.or(""));
        Collections.sort(dataSetNames);
        try {

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
            return dataSets;

        } catch (Throwable e) {
            logger.error("Error getting metrics for params: zeroes["+zeroes+"], metricName[" + metricName + "], dataType[" + dataType + "], period["+dataType+"], scope["+scope+"], dataSet["+dataSetNames+"]", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    @Path("/options")
    public AvailableOptionsVO getMetricsOptions() {

        try {
            return metricsOptionsHandler.getAvailableOptions();
        } catch (Throwable e) {
            logger.error("Error getting available metrics options", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
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
