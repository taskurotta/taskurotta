package ru.taskurotta.dropwizard.resources.monitoring;

import ru.taskurotta.service.storage.InterruptedTasksService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: stukushin
 * Date: 10.08.2015
 * Time: 16:31
 */

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/monitoring/task/knowinterruptedcount")
public class KnowInterruptedTasksCountResource {

    private InterruptedTasksService interruptedTasksService;

    @GET
    public Response getKnowInterruptedTasksCount() {
        int count = interruptedTasksService.getKnowInterruptedTasksCount();
        return Response.ok(count, MediaType.APPLICATION_JSON).build();
    }

    public void setInterruptedTasksService(InterruptedTasksService interruptedTasksService) {
        this.interruptedTasksService = interruptedTasksService;
    }
}
