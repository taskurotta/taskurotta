package ru.taskurotta.dropwizard.resources.console;

import ru.taskurotta.backend.console.model.QueueVO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Resource for obtaining queue list info
 * User: dimadin
 * Date: 21.05.13 11:49
 */
@Path("/console/queues")
public class QueueListResource extends BaseResource {

    @GET
    public Response getQueuesInfo() {
        try {
            List<QueueVO> queuesState = consoleManager.getQueuesState();
            logger.debug("QueueState getted is [{}]", queuesState);
            return Response.ok(queuesState, MediaType.APPLICATION_JSON).build();
        } catch(Throwable e) {
            logger.error("Error at getting queues list", e);
            return Response.serverError().build();
        }
    }

}
