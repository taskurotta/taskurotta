package ru.taskurotta.dropwizard.resources.console.queue;

import com.google.common.base.Optional;
import ru.taskurotta.dropwizard.resources.console.BaseResource;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.queue.TaskQueueItem;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for obtaining info for task in queue
 * User: dimadin
 * Date: 21.05.13 12:44
 */
@Deprecated
@Path("/console/queue/{name}")
public class QueueContentResource extends BaseResource {

    private static int DEFAULT_START_PAGE = 1;
    private static int DEFAULT_PAGE_SIZE = 10;

    @GET
    public Response getQueueInfo(@PathParam("name") String name, @QueryParam("pageNum") Optional<Integer> pageNum, @QueryParam("pageSize") Optional<Integer> pageSize) {
        try {
            GenericPage<TaskQueueItem> queuesTasks = consoleManager.getEnqueueTasks(name, pageNum.or(DEFAULT_START_PAGE), pageSize.or(DEFAULT_PAGE_SIZE));
            logger.debug("Queue[{}] content getted is [{}]", name, queuesTasks);
            return Response.ok(queuesTasks, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting queue[" + name + "] content", e);
            return Response.serverError().build();
        }
    }

}
