package ru.taskurotta.dropwizard.resources.console;

import com.google.common.base.Optional;
import ru.taskurotta.backend.console.model.QueueVO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * User: moroz
 * Date: 11.06.13
 */
@Path("/console/hoveringQueues")
public class HoveringQueuesResource extends BaseResource {

    private static float DEFAULT_PERIOD_SIZE = 2;

    @GET
    public Response getQueuesInfo(@QueryParam("periodSize") Optional<Float> periodSize) {
        try {
            List<QueueVO> queuesState = consoleManager.getQueuesHovering(periodSize.or(DEFAULT_PERIOD_SIZE));
            logger.debug("QueueState getted is [{}]", queuesState);
            return Response.ok(queuesState, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting queues list", e);
            return Response.serverError().build();
        }
    }
}
