package ru.taskurotta.dropwizard.resources.console;

import com.google.common.base.Optional;
import ru.taskurotta.backend.console.model.ActorVO;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

/**
 * User: stukushin, dimadin
 * Date: 04.09.13 17:06
 */
@Path("/console/actor/{action}")
public class ActorResource extends BaseResource {

    public static final String ACTION_BLOCK = "block";
    public static final String ACTION_UNBLOCK = "unblock";
    public static final String ACTION_LIST = "list";


    @POST
    public Response blockActor(String actorId, @PathParam("action") String action) {
        try {
            if (ACTION_BLOCK.equals(action)) {
                consoleManager.blockActor(actorId);
            } else if (ACTION_UNBLOCK.equals(action)) {
                consoleManager.unblockActor(actorId);
            } else {
                logger.error("Unknown actor action["+action+"] getted");
                return Response.serverError().build();
            }
            return Response.ok().build();
        } catch (Throwable e) {
            logger.error("Catch exception while ["+action+"] actor [" + actorId +"]", e);
            return Response.serverError().build();
        }
    }

    @GET
    public Response listActors(@PathParam("action") String action,
                               @QueryParam("pageNum") Optional<Integer> pageNum, @QueryParam("pageSize") Optional<Integer> pageSize) {

        try {
            if (ACTION_LIST.equals(action)) {
                Collection<ActorVO> actors = consoleManager.getActorList(pageNum.or(1), pageSize.or(5));
                logger.debug("Actor list getted is [{}]", actors);
                return Response.ok(actors, MediaType.APPLICATION_JSON).build();
            } else {
                logger.error("Unknown actor action["+action+"] getted");
                return Response.serverError().build();
            }
        } catch (Throwable e) {
            logger.error("Error for action ["+action+"]", e);
            return Response.serverError().build();
        }

    }

}
