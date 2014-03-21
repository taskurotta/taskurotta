package ru.taskurotta.dropwizard.resources.console.operation;

import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.service.gc.GarbageCollectorService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Javadoc should be here
 * Date: 14.01.14 12:24
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/operation/gc")
public class GarbageCollectorResource {

    private GarbageCollectorService gcService;

    @GET
    @Path("/size")
    public Integer getGcServiceQueueSize() {
        return gcService.getCurrentSize();
    }

    @Required
    public void setGcService(GarbageCollectorService gcService) {
        this.gcService = gcService;
    }
}
