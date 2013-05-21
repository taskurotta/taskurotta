package ru.taskurotta.console.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Resource for obtaining info for task in queue
 * User: dimadin
 * Date: 21.05.13 12:44
 */
@Path("/info/queue/{name}")
public class QueueResource extends BaseResource {

    @GET
    public Response getQueueInfo(@PathParam("name")String name) {
        return Response.ok().build();
    }




}
