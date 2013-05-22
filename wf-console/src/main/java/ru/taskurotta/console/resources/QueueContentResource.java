package ru.taskurotta.console.resources;

import ru.taskurotta.console.model.QueuedTaskVO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Resource for obtaining info for task in queue
 * User: dimadin
 * Date: 21.05.13 12:44
 */
@Path("/console/queue/{name}")
public class QueueContentResource extends BaseResource {

    @GET
    public Response getQueueInfo(@PathParam("name")String name) {
        try {
            List<QueuedTaskVO> queuesTasks = consoleManager.getEnqueueTasks(name);
            logger.debug("Queue[{}] content getted is [{}]", name, queuesTasks);
            return Response.ok(queuesTasks, MediaType.APPLICATION_JSON).build();
        } catch(Throwable e) {
            logger.error("Error at getting queue["+name+"] content", e);
            return Response.serverError().build();
        }
    }




}
