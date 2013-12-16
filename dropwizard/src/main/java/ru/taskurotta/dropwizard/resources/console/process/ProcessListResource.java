package ru.taskurotta.dropwizard.resources.console.process;

import com.google.common.base.Optional;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.ProcessVO;
import ru.taskurotta.dropwizard.resources.console.BaseResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: moroz
 * Date: 31.05.13
 */

@Path("/console/processes")
public class ProcessListResource extends BaseResource {
    private static int DEFAULT_START_PAGE = 1;
    private static int DEFAULT_PAGE_SIZE = 10;

    @GET
    public Response getProcessInfo(@QueryParam("pageNum") Optional<Integer> pageNum, @QueryParam("pageSize") Optional<Integer> pageSize) {
        try {
            int pgSize = pageSize.or(-1);
            GenericPage<ProcessVO> processes = consoleManager.listProcesses(pageNum.or(DEFAULT_START_PAGE), pageSize.or(DEFAULT_PAGE_SIZE));
            logger.debug("Processes getted is [{}]", processes);
            return Response.ok(processes, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting processes list", e);
            return Response.serverError().build();
        }
    }

}
