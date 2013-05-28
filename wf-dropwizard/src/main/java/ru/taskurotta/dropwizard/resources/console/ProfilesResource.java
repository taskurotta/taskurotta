package ru.taskurotta.dropwizard.resources.console;

import ru.taskurotta.backend.console.model.ProfileVO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 28.05.13 10:58
 */
@Path("/console/profiles")
public class ProfilesResource extends BaseResource {

    @GET
    public Response getProcess() {

        try {
            List<ProfileVO> profiles = consoleManager.getProfilesInfo();
            logger.debug("Profile info getted is [{}]", profiles);
            return Response.ok(profiles, MediaType.APPLICATION_JSON).build();
        } catch(Throwable e) {
            logger.error("Error at getting profiles info", e);
            return Response.serverError().build();
        }

    }

}
