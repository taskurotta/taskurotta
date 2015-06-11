package ru.taskurotta.service.notification;

import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.Subscription;

import java.util.Collection;

/**
 * Created on 08.06.2015.
 */
public interface NotificationManager {

    void execute();

    Subscription getSubscription(long id);

    NotificationTrigger getTrigger(long id);

    long addSubscription(Subscription cfg);

    void updateSubscription(Subscription cfg, long id);

    Collection<Subscription> listSubscriptions();

    Collection<NotificationTrigger> listTriggers();

}
