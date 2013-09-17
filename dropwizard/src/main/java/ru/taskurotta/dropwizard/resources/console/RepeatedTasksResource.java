package ru.taskurotta.dropwizard.resources.console;

import com.google.common.base.Optional;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

/**
 * User: moroz
 * Date: 11.06.13
 */
@Path("/console/repeatedTasks")
public class RepeatedTasksResource extends BaseResource {

    private static int DEFAULT_ITERATION_COUNT = 5;

    @GET
    public Response getQueuesInfo(@QueryParam("iterationCount") Optional<Integer> iterationCount) {
        try {
            Collection<TaskContainer> tasks = consoleManager.getRepeatedTasks(iterationCount.or
                    (DEFAULT_ITERATION_COUNT));

            logger.debug("Tasks getted is [{}]", tasks);
            return Response.ok(tasks, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting Tasks list", e);
            return Response.serverError().build();
        }
    }
}
