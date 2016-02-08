package ru.taskurotta.dropwizard.resources.console.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.service.notification.NotificationManager;
import ru.taskurotta.service.notification.model.NotificationTrigger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;

/**
 * Created on 24.08.2015.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/triggers")
public class TriggersResource {

    private static final Logger logger = LoggerFactory.getLogger(TriggersResource.class);

    private NotificationManager notificationManager;

    @GET
    public Collection<NotificationTrigger> listTriggers() {
        return notificationManager.listTriggers();
    }

    @Required
    public void setNotificationManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }
}
