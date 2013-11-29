package ru.taskurotta.dropwizard.resources.console.process;

import ru.taskurotta.dropwizard.resources.console.BaseResource;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 24.05.13 12:48
 */
@Path("/console/tasks/process/{processId}")
public class ProcessTaskResource extends BaseResource {

    @GET
    public Response getProcessTasks(@PathParam("processId") String processId) {

        try {
            Collection<TaskContainer> tasks = consoleManager.getProcessTasks(UUID.fromString(processId));
            logger.debug("Task list getted by processId[{}] is [{}]", processId, tasks);
            return Response.ok(tasks, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting process tasks by processId[" + processId + "]", e);
            return Response.serverError().build();
        }

    }

}
