package ru.taskurotta.dropwizard.resources.console.meta;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created on 20.08.2014.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/timer")
public class TimerResource {

    @GET
    @Path("/server_time")
    public long getServerTime() {
        return System.currentTimeMillis();
    }

}
