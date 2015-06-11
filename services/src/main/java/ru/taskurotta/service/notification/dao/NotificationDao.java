package ru.taskurotta.service.notification.dao;

import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.Subscription;

import java.util.Collection;

/**
 * Created on 10.06.2015.
 */
public interface NotificationDao {

    Subscription getSubscription(long id);

    NotificationTrigger getTrigger(long id);

    long addSubscription(Subscription subscription);

    long addTrigger(NotificationTrigger trigger);

    void updateSubscription(Subscription subscription, long id);

    void updateTrigger(NotificationTrigger trigger, long id);

    Collection<Subscription> listSubscriptions();

    Collection<NotificationTrigger> listTriggers();

    Collection<Subscription> listTriggerSubscriptions(NotificationTrigger trigger);

    Collection<Long> listTriggerKeys();



}
