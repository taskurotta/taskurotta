package ru.taskurotta.service.notification;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.notification.handler.TriggerHandler;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.SearchCommand;
import ru.taskurotta.service.notification.model.Subscription;

import java.util.Collection;

/**
 * Created on 08.06.2015.
 */
public interface NotificationManager {

    /**
     * Runs all checks for registered triggers and send emails id any problem detected
     */
    void execute();

    Subscription getSubscription(long id);

    NotificationTrigger getTrigger(long id);

    long addSubscription(Subscription subscription);

    void removeSubscription(long id);

    long addTrigger(NotificationTrigger trigger);

    void updateSubscription(Subscription cfg, long id);

    Collection<Subscription> listSubscriptions();

    Collection<NotificationTrigger> listTriggers();

    Collection<TriggerHandler> listHandlers();

    GenericPage<Subscription> listSubscriptions(SearchCommand command);

}
