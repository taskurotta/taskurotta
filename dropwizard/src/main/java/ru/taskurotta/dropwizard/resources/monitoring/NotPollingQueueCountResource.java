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
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

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

    private static final String newString = "\r\n";

    @GET
    public Response getNotPollingQueueCount() {
        MetricsDataHandler metricsDataHandler = MetricsDataHandler.getInstance();
        TreeMap<Date, String> notPullingQueues = new TreeMap<>(new Comparator<Date>() {
            @Override
            public int compare(Date date1, Date date2) {
                return date2.compareTo(date1);
            }
        });

        Collection<String> queueNames = queueService.getQueueNames();
        long now = System.currentTimeMillis();
        for (String queueName : queueNames) {
            Date lastActivity = metricsDataHandler.getLastActivityTime(MetricName.POLL.getValue(), queueName);
            if (lastActivity == null || (now - lastActivity.getTime()) > pollTimeout) {
                notPullingQueues.put(lastActivity == null ? new Date(0) : lastActivity, queueName);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(notPullingQueues.size()).append(newString);
        for (Map.Entry<Date, String> entry : notPullingQueues.entrySet()) {
            stringBuilder.append(entry.getValue()).append(" last pulled at ").append(entry.getKey()).append(newString);
        }

        return Response.ok(stringBuilder.toString(), MediaType.APPLICATION_JSON).build();
    }

    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public void setQueueService(QueueService queueService) {
        this.queueService = queueService;
    }
}
