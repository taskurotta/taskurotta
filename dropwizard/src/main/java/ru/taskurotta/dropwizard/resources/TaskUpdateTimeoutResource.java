package ru.taskurotta.dropwizard.resources;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.TaskServerResource;
import ru.taskurotta.transport.model.UpdateTimeoutRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(TaskServerResource.UPDATE_TIMEOUT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskUpdateTimeoutResource  extends TaskServerAbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(TaskReleaserResource.class);

    private TaskServer taskServer;

    @POST
    public Response updateTimeout(UpdateTimeoutRequest updateTimeoutRequest) {
        logger.debug("update timeout called for [{}]", updateTimeoutRequest);

        try {
            taskServer.updateTaskTimeout(updateTimeoutRequest.getTaskId(),
                    updateTimeoutRequest.getProcessId(), updateTimeoutRequest.getTimeout());
            logger.debug("Timeout successfully updated for [{}]", updateTimeoutRequest);
        } catch (Throwable e) {
            GeneralTaskServer.errorsCounter.incrementAndGet();
            logError("Update timeout of task failed!", e);

            return Response.serverError().build();
        }

        return Response.ok().build();

    }

    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }
}