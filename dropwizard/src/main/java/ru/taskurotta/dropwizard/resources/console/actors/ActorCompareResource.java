package ru.taskurotta.dropwizard.resources.console.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.service.console.manager.ActorConfigManager;
import ru.taskurotta.service.console.model.MetricsStatDataVO;
import ru.taskurotta.service.console.retriever.metrics.MetricsMethodDataRetriever;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Resource providing data on actor metrics for comparison
 * User: dimadin
 * Date: 04.10.13 10:24
 */
@Path("/console/actor/metrics/compare")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ActorCompareResource {

    private static final Logger logger = LoggerFactory.getLogger(ActorCompareResource.class);
    private MetricsMethodDataRetriever metricsDataRetriever;
    private ActorConfigManager actorConfigManager;

    @POST
    public Response getMetricsDataOnActors(CompareCommand command) {

        Map<String, Collection<MetricsStatDataVO>>  result = null;

        if (isContainsData(command)) {
            result = actorConfigManager.getMetricsData(command.getMetrics(), command.getActorIds());
        }

        logger.debug("Metrics data getted by command [{}] is [{}]", command, result);
        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }


    private boolean isContainsData(CompareCommand command) {
        return command!=null
                && command.getActorIds() != null
                && command.getMetrics() != null
                && !command.getActorIds().isEmpty() && !command.getMetrics().isEmpty();
    }

    @GET
    public Response getAvailableMetricsList() {
        Collection<String> metrics = metricsDataRetriever.getMetricNames();
        logger.debug("Metrics getted are [{}]", metrics);
        return Response.ok(metrics, MediaType.APPLICATION_JSON).build();
    }

    public static class CompareCommand implements Serializable {
        private List<String> actorIds;
        private List<String> metrics;

        public List<String> getActorIds() {
            return actorIds;
        }

        public void setActorIds(List<String> actorIds) {
            this.actorIds = actorIds;
        }

        public List<String> getMetrics() {
            return metrics;
        }

        public void setMetrics(List<String> metrics) {
            this.metrics = metrics;
        }

        @Override
        public String toString() {
            return "CompareCommand{" +
                    "actorIds=" + actorIds +
                    ", metrics=" + metrics +
                    "} ";
        }
    }


    @Required
    public void setMetricsDataRetriever(MetricsMethodDataRetriever metricsDataRetriever) {
        this.metricsDataRetriever = metricsDataRetriever;
    }

    @Required
    public void setActorConfigManager(ActorConfigManager actorConfigManager) {
        this.actorConfigManager = actorConfigManager;
    }
}
