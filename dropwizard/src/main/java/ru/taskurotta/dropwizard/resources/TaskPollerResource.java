package ru.taskurotta.dropwizard.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.TaskServerResource;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path(TaskServerResource.POLL)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskPollerResource extends TaskServerAbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(TaskPollerResource.class);

    private TaskServer taskServer;
    private boolean partitionRoutingEnabled;
    private String partitionRoutingPort;

    public TaskPollerResource(TaskServer taskServer, boolean partitionRoutingEnabled, String partitionRoutingPort) {
        this.taskServer = taskServer;
        this.partitionRoutingEnabled = partitionRoutingEnabled;
        this.partitionRoutingPort = partitionRoutingPort;
    }

    @POST
    public Response poll(ActorDefinition actorDefinition) throws Exception {
        logger.debug("poll called with entity[{}]", actorDefinition);

        TaskContainer result = null;

        try {
            try {
                result = taskServer.poll(actorDefinition);
                logger.debug("Task polled for[{}] is[{}]", actorDefinition.getName(), result);
            } catch (Throwable e) {
                GeneralTaskServer.errorsCounter.incrementAndGet();
                logError("Poll task failed!", e);

                return Response.serverError().build();
            }


            Response.ResponseBuilder responseBuilder = null;
            if (result == null) {
                responseBuilder = Response.noContent();
            } else {
                responseBuilder = Response.ok(result, MediaType.APPLICATION_JSON);

                if (partitionRoutingEnabled) {
                    String processOwner = TaskServer.processOwner.get();
                    if (processOwner != null) {
                        responseBuilder.header(TaskServer.FIELD_PROCESS_OWNER, createEndpoint(processOwner));
                    }
                }
            }

            if (partitionRoutingEnabled) {
                String queueOwner = TaskServer.queueOwner.get();
                if (queueOwner != null) {
                    responseBuilder.header(TaskServer.FIELD_QUEUE_OWNER, createEndpoint(queueOwner));
                }
            }

            return responseBuilder.build();

        } finally {
            TaskServer.queueOwner.remove();
        }

    }


    private String createEndpoint(String host) {
        return "http://" + host + ":" + partitionRoutingPort;
    }

}
