package ru.taskurotta.dropwizard.resources;

import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.TaskContainerWrapper;
import ru.taskurotta.server.TaskServer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/tasks/start")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskStarterResource {

    private static final Logger logger = LoggerFactory.getLogger(TaskStarterResource.class);
    private TaskServer taskServer;

    @POST
    @Timed
    public Response startProcess(TaskContainerWrapper taskContainerWrapper) {
        logger.debug("startProcess resource called with entity[{}]", taskContainerWrapper);

        try {
            taskServer.startProcess(taskContainerWrapper.getTaskContainer());
            logger.debug("Successfully started process task[{}]", taskContainerWrapper.getTaskContainer());
        } catch(Throwable e) {
            logger.error("Starting of process by task["+taskContainerWrapper+"] failed!", e);
            return Response.serverError().build();
        }

        return Response.ok().build();

    }

    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

}
