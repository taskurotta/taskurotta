package ru.taskurotta.service.notification;

import ru.taskurotta.service.notification.handler.TriggerHandler;

import java.util.Collection;

/**
 * Created on 16.06.2015.
 */
public interface NotificationHandlersProvider {

    Collection<TriggerHandler> getHandlers();

}
