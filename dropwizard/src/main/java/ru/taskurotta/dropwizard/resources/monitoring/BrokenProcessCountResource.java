package ru.taskurotta.dropwizard.resources.monitoring;

import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: stukushin
 * Date: 22.04.2015
 * Time: 13:12
 */

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/monitoring/process/brokencount")
public class BrokenProcessCountResource {

    private ProcessInfoRetriever processInfoRetriever;

    @GET
    public Response getBrokenProcessCount() {
        int count = processInfoRetriever.getBrokenProcessCount();
        return Response.ok(count, MediaType.APPLICATION_JSON).build();
    }

    public void setProcessInfoRetriever(ProcessInfoRetriever processInfoRetriever) {
        this.processInfoRetriever = processInfoRetriever;
    }
}
