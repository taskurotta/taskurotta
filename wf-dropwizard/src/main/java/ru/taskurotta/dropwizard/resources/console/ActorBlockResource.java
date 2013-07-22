package ru.taskurotta.dropwizard.resources.console;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * User: stukushin
 * Date: 22.07.13
 * Time: 13:31
 */

@Path("/console/actor/block/{actorId}")
public class ActorBlockResource extends BaseResource {

    @POST
    public Response blockActor(@PathParam("actorId") String actorId) {
        try {
            consoleManager.blockActor(actorId);
            return Response.ok().build();
        } catch (Throwable e) {
            logger.error("Catch exception while block actor [" + actorId +"]", e);
            return Response.serverError().build();
        }
    }
}
