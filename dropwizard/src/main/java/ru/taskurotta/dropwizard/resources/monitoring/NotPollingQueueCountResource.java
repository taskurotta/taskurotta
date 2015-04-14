package ru.taskurotta.dropwizard.resources.monitoring;

import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.queue.QueueService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;

/**
 * User: stukushin
 * Date: 13.04.2015
 * Time: 17:11
 */

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/monitoring/queue/notpollingcount")
public class NotPollingQueueCountResource {

    private long pollTimeout;
    private QueueService queueService;

    @GET
    public Response getNotPollingQueueCount() {
        MetricsDataHandler metricsDataHandler = MetricsDataHandler.getInstance();

        int count = 0;
        long now = System.currentTimeMillis();
        Collection<String> queueNames = queueService.getQueueNames();
        for (String name : queueNames) {
            Date lastActivity = metricsDataHandler.getLastActivityTime(MetricName.POLL.getValue(), name);
            if (lastActivity == null) {
                continue;
            }

            if ((now - lastActivity.getTime()) > pollTimeout) {
                count++;
            }
        }

        return Response.ok(count, MediaType.APPLICATION_JSON).build();
    }

    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public void setQueueService(QueueService queueService) {
        this.queueService = queueService;
    }
}
