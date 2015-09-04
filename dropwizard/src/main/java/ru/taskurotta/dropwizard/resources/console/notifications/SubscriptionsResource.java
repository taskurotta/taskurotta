package ru.taskurotta.dropwizard.resources.console.notifications;

import com.google.common.base.Optional;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.dropwizard.resources.console.Status;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.notification.NotificationManager;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.SearchCommand;
import ru.taskurotta.service.notification.model.Subscription;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created on 15.06.2015.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/subscriptions")
public class SubscriptionsResource {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionsResource.class);

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
        Subscription result = notificationManager.getSubscription(id);
        logger.debug("Subscription got by id[{}] is [{}]", id, result);
        return result;
    }

    @PUT
    public Status addSubscription(SubscriptionCommand command) {
        logger.debug("addSubscription triggered with params: command[{}]", command);
        Subscription subscription = asSubscription(-1l, command.actorIds, command.emails, command.triggersKeys);
        if (subscription!=null) {
            long result = notificationManager.addSubscription(subscription);
            return new Status(HttpStatus.CREATED_201, String.valueOf(result));
        } else {
            return new Status(HttpStatus.BAD_REQUEST_400, "Missing required params: [emails], [actorIds]");
        }
    }

    @POST
    public Status updateSubscription(SubscriptionCommand sub) {
        if (sub.id>0) {
            notificationManager.updateSubscription(asSubscription(sub.id, sub.actorIds, sub.emails, sub.triggersKeys), sub.id);
            return new Status(HttpStatus.OK_200, "");
        } else {
            return new Status(HttpStatus.BAD_REQUEST_400, "Missing required param: [id]");
        }
    }

    public static class SubscriptionCommand implements Serializable {
        public long id;
        public String actorIds;
        public String emails;
        public List<Long> triggersKeys;

        @Override
        public String toString() {
            return "SubscriptionCommand{" +
                    "id=" + id +
                    ", actorIds='" + actorIds + '\'' +
                    ", emails='" + emails + '\'' +
                    ", triggersKeys=" + triggersKeys +
                    '}';
        }
    }

    Subscription asSubscription(long id, String actorIds, String emails, List<Long> triggerKeys) {
        Subscription result = null;
        if (actorIds!=null && emails!=null) {
            result = new Subscription();
            result.setChangeDate(new Date());
            result.setTriggersKeys(triggerKeys);
            result.setEmails(Arrays.asList(emails.split(",\\s*")));
            result.setActorIds(Arrays.asList(actorIds.split(",\\s*")));
            result.setId(id);
        }
        return result;
    }

//    List<Long> getAllTriggerKeys() {
//        List<Long> result = new ArrayList<>();
//        Collection<NotificationTrigger> triggers = notificationManager.listTriggers();
//        if (triggers!=null && !triggers.isEmpty()) {
//            for (NotificationTrigger t : triggers) {
//                result.add(t.getId());
//            }
//        }
//        return result;
//    }

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
