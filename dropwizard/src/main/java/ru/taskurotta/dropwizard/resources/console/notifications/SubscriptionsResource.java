package ru.taskurotta.dropwizard.resources.console.notifications;

import com.google.common.base.Optional;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.dropwizard.resources.console.Status;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.notification.NotificationManager;
import ru.taskurotta.service.notification.model.SearchCommand;
import ru.taskurotta.service.notification.model.Subscription;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Created on 15.06.2015.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/subscriptions")
public class SubscriptionsResource {
    private static int DEFAULT_PAGE_NUM = 1;
    private static int DEFAULT_PAGE_SIZE = 10;

    private NotificationManager notificationManager;

    @GET
    public GenericPage<Subscription> listSubscriptions(@QueryParam("pageNum") Optional<Integer> pageNum,
                                                       @QueryParam("pageSize") Optional<Integer> pageSize) {
        SearchCommand command = new SearchCommand();
        command.setPageNum(pageNum.or(DEFAULT_PAGE_NUM));
        command.setPageSize(pageSize.or(DEFAULT_PAGE_SIZE));
        return notificationManager.listSubscriptions(command);
    }

    @GET
    @Path("/{id}")
    public Subscription getSubscription(@PathParam("id") Long id) {
        return notificationManager.getSubscription(id);
    }

    @POST
    public Status addSubscription(Subscription sub) {
        long result = notificationManager.addSubscription(sub);
        return new Status(HttpStatus.OK_200, String.valueOf(result));
    }

    @POST
    @Path("/{id}")
    public Status updateSubscription(@PathParam("id") Long id, Subscription sub) {
        notificationManager.updateSubscription(sub, id);
        return new Status(HttpStatus.OK_200, "");
    }

    @DELETE
    @Path("/{id}")
    public Status removeSubscription(@PathParam("id") Long id) {
        notificationManager.removeSubscription(id);
        return new Status(HttpStatus.OK_200, "");
    }

    @Required
    public void setNotificationManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }
}
