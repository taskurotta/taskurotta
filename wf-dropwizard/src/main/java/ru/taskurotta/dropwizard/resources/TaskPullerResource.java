package ru.taskurotta.dropwizard.resources;

import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.dropwizard.client.serialization.wrapper.ActorDefinitionWrapper;
import ru.taskurotta.dropwizard.client.serialization.wrapper.TaskContainerWrapper;
import ru.taskurotta.server.TaskServer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/tasks/poll")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskPullerResource {
    private static final Logger logger = LoggerFactory.getLogger(TaskPullerResource.class);
    private TaskServer taskServer;

    @POST
    @Timed
    public Response pullAction(ActorDefinitionWrapper actorDefinitionWrapper) throws Exception {
        logger.debug("pullAction called with entity[{}]", actorDefinitionWrapper);

        TaskContainer result = null;

        try {
            result = taskServer.poll(actorDefinitionWrapper.getActorDefinition());
            logger.debug("Task pulled for[{}] is[{}]",actorDefinitionWrapper.getActorDefinition().getName(), result);
        } catch (Throwable e) {
            logger.error("Pulling task for[" + actorDefinitionWrapper + "] failed!", e);
            return Response.serverError().build();
        }

        return Response.ok(new TaskContainerWrapper(result), MediaType.APPLICATION_JSON).build();

    }

    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

}
