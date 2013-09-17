package ru.taskurotta.dropwizard.resources.console;

import com.google.common.base.Optional;
import ru.taskurotta.backend.console.model.ProcessVO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * User: moroz
 * Date: 05.06.13
 */
@Path("/console/process/search")
public class ProcessSearchResource extends BaseResource {

    public static final String DEFAULT_TYPE = "process_id";


    @GET
    public Response getProcessInfo(@QueryParam("processId") Optional<String> processId, @QueryParam("customId") Optional<String> customId) {
        try {
            List<ProcessVO> processes = consoleManager.findProcesses(processId.or(""), customId.or(""));
            logger.debug("Processes getted is [{}]", processes);
            return Response.ok(processes, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting processes list", e);
            return Response.serverError().build();
        }

    }
}
