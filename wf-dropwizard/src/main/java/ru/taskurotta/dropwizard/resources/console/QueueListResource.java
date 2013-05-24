package ru.taskurotta.dropwizard.resources.console;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueueVO;

/**
 * Resource for obtaining queue list info
 * User: dimadin
 * Date: 21.05.13 11:49
 */
@Path("/console/queues")
public class QueueListResource extends BaseResource {

    private static int DEFAULT_START_PAGE = 1;
    private static int DEFAULT_PAGE_SIZE = 10;

    @GET
    public Response getQueuesInfo(@QueryParam("pageNum") Optional<Integer> pageNum, @QueryParam("pageSize") Optional<Integer> pageSize) {
        try {
            int pgSize = pageSize.or(-1);
            GenericPage<QueueVO> queuesState = consoleManager.getQueuesState(pageNum.or(DEFAULT_START_PAGE), pageSize.or(DEFAULT_PAGE_SIZE));
            logger.debug("QueueState getted is [{}]", queuesState);
            return Response.ok(queuesState, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting queues list", e);
            return Response.serverError().build();
        }
    }
}
