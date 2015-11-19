package ru.taskurotta.dropwizard.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.TaskServerResource;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path(TaskServerResource.START)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskStarterResource extends TaskServerAbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(TaskStarterResource.class);

    private TaskServer taskServer;

    @POST
    public Response startProcess(TaskContainer taskContainer) {
        logger.debug("startProcess resource called with entity[{}], generateId[{}]", taskContainer);

        taskContainer = injectIdsIfAbsent(taskContainer);

        try {
            taskServer.startProcess(taskContainer);
            logger.debug("Successfully started process task[{}]", taskContainer);
        } catch (Throwable e) {
            GeneralTaskServer.errorsCounter.incrementAndGet();
            logError("Starting of process by task failed!", e);
            return Response.serverError().build();
        }

        return Response.ok().build();

    }

    private TaskContainer injectIdsIfAbsent(TaskContainer target) {
        if (target.getProcessId() != null && target.getTaskId() != null) { //has guids already set
            return target;
        }
        UUID taskId = target.getTaskId() != null ? target.getTaskId() : UUID.randomUUID();
        UUID processId = target.getProcessId() != null ? target.getProcessId() : UUID.randomUUID();
        return new TaskContainer(taskId, processId, target.getPass(), target.getMethod(), target.getActorId(), target
                .getType(), target.getStartTime(), target.getErrorAttempts(), target.getArgs(), target.getOptions(),
                target.isUnsafe(), target.getFailTypes());
    }

    @Required
    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

}
