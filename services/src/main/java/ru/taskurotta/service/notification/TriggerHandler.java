package ru.taskurotta.service.notification;

import ru.taskurotta.service.notification.model.Subscription;

import java.util.Collection;

/**
 * Created on 10.06.2015.
 */
public interface TriggerHandler {

    String handleTrigger(String stateJson, Collection<Subscription> subscriptions, String cfgJson);

    String getName();

}
