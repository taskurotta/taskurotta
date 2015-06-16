package ru.taskurotta.dropwizard.resources.monitoring;

import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Map;

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
    private QueueInfoRetriever queueInfoRetriever;

    private static final String newString = "\r\n";

    @GET
    public Response getNotPollingQueueCount() {
        StringBuilder stringBuilder = new StringBuilder();
        Map<Date, String> notPullingQueues = queueInfoRetriever.getNotPollingQueues(pollTimeout);
        if (notPullingQueues != null) {
            stringBuilder.append(notPullingQueues.size()).append(newString);
            for (Map.Entry<Date, String> entry : notPullingQueues.entrySet()) {
                stringBuilder.append(entry.getValue()).append(" last pulled at ").append(entry.getKey()).append(newString);
            }
        }

        return Response.ok(stringBuilder.toString(), MediaType.APPLICATION_JSON).build();
    }

    @Required
    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    @Required
    public void setQueueInfoRetriever(QueueInfoRetriever queueInfoRetriever) {
        this.queueInfoRetriever = queueInfoRetriever;
    }
}
