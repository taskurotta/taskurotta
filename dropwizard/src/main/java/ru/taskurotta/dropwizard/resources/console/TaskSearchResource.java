package ru.taskurotta.dropwizard.resources.console;

import com.google.common.base.Optional;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * User: dimadin
 * Date: 10.09.13 17:29
 */
@Path("/console/task/search")
public class TaskSearchResource extends BaseResource {

    @GET
    public Response getTask(@QueryParam("taskId") Optional<String> taskId, @QueryParam("processId") Optional<String> processId) {

        try {
            List<TaskContainer> result = consoleManager.findTasks(processId.or(""), taskId.or(""));
            logger.debug("Task found by id[{}], processId[{}] is  [{}]", taskId, processId, result);
            return Response.ok(result, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting task by id["+taskId+"], processId["+processId+"]", e);
            return Response.serverError().build();
        }

    }

}
