package ru.taskurotta.dropwizard.resources.console.process;

import com.google.common.base.Optional;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.dropwizard.resources.console.BaseResource;

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
            List<Process> processes = consoleManager.findProcesses(processId.or(""), customId.or(""));
            logger.debug("Processes getted is [{}]", processes);
            return Response.ok(processes, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting processes list", e);
            return Response.serverError().build();
        }

    }
}
