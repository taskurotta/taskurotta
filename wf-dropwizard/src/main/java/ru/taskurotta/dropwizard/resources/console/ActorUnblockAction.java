package ru.taskurotta.dropwizard.resources.console;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * User: stukushin
 * Date: 22.07.13
 * Time: 16:27
 */

@Path("/console/actor/unblock")
public class ActorUnblockAction extends BaseResource {

    @POST
    public Response blockActor(String actorId) {
        try {
            consoleManager.unblockActor(actorId);
            return Response.ok().build();
        } catch (Throwable e) {
            logger.error("Catch exception while block actor [" + actorId +"]", e);
            return Response.serverError().build();
        }
    }
}