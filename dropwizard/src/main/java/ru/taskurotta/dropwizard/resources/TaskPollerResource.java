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
public class TaskPollerResource {
    private static final Logger logger = LoggerFactory.getLogger(TaskPollerResource.class);
    private TaskServer taskServer;

    @POST
    public Response poll(ActorDefinition actorDefinition) throws Exception {
        logger.debug("poll called with entity[{}]", actorDefinition);

        TaskContainer result = null;

        try {
            result = taskServer.poll(actorDefinition);
            logger.debug("Task polled for[{}] is[{}]", actorDefinition.getName(), result);
        } catch (Throwable e) {
            GeneralTaskServer.errorsCounter.incrementAndGet();
            logger.error("Poll task failed! ActorDefinition = [" + actorDefinition + "] ", e);

            return Response.serverError().build();
        }

        if (result == null) {
            return Response.noContent().build();
        }

        return Response.ok(result, MediaType.APPLICATION_JSON).build();

    }

    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

}
