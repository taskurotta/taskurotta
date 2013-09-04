package ru.taskurotta.dropwizard.resources;

import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.dropwizard.TaskurottaResource;
import ru.taskurotta.dropwizard.client.serialization.wrapper.ActorDefinitionWrapper;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(TaskurottaResource.POLL)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskPollerResource {
    private static final Logger logger = LoggerFactory.getLogger(TaskPollerResource.class);
    private TaskServer taskServer;

    @POST
    @Timed
    public Response poll(ActorDefinitionWrapper actorDefinitionWrapper) throws Exception {
        logger.debug("poll called with entity[{}]", actorDefinitionWrapper);

        TaskContainer result = null;

        try {
            result = taskServer.poll(actorDefinitionWrapper.getActorDefinition());
            logger.debug("Task polled for[{}] is[{}]",actorDefinitionWrapper.getActorDefinition().getName(), result);
        } catch (Throwable e) {
            logger.error("Poll task for[" + actorDefinitionWrapper + "] failed!", e);
            return Response.serverError().build();
        }

        return Response.ok(result, MediaType.APPLICATION_JSON).build();

    }

    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

}
