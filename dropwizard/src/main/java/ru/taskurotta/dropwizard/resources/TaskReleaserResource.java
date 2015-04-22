package ru.taskurotta.dropwizard.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.TaskServerResource;
import ru.taskurotta.transport.model.DecisionContainer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(TaskServerResource.RELEASE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskReleaserResource {

    private static final Logger logger = LoggerFactory.getLogger(TaskReleaserResource.class);
    private TaskServer taskServer;

    @POST
    public Response release(DecisionContainer resultContainer) {
        logger.debug("release resource called with entity[{}]", resultContainer);

        try {
            taskServer.release(resultContainer);
            logger.debug("Task successfully released, [{}]", resultContainer);
        } catch(Throwable e) {
            logger.error("Releasing of task["+resultContainer+"] failed!", e);
            return Response.serverError().build();
        }

        return Response.ok().build();

    }

    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }


}
