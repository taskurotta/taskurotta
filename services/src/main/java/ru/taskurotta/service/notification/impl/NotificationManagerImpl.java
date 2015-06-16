package ru.taskurotta.service.notification.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.notification.NotificationHandlersProvider;
import ru.taskurotta.service.notification.NotificationManager;
import ru.taskurotta.service.notification.dao.NotificationDao;
import ru.taskurotta.service.notification.handler.TriggerHandler;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.SearchCommand;
import ru.taskurotta.service.notification.model.Subscription;

import java.util.Collection;
import java.util.Date;

/**
 * Created on 08.06.2015.
 */
public class NotificationManagerImpl implements NotificationManager {

    private static final Logger logger = LoggerFactory.getLogger(NotificationManagerImpl.class);

    private NotificationDao notificationDao;

    private Collection<TriggerHandler> handlers;

    private NotificationHandlersProvider notificationHandlersProvider;

    private boolean enabled = false;

    public NotificationManagerImpl(NotificationDao notificationDao, NotificationHandlersProvider notificationHandlersProvider, boolean enabled) {
        this.notificationDao = notificationDao;
        this.notificationHandlersProvider = notificationHandlersProvider;
        this.enabled = enabled;
        if (!enabled) {
            logger.warn("Notifications are disabled");
        }

    }

    public void init() {
        if (enabled) {
            this.handlers = notificationHandlersProvider.getHandlers();

            if (handlers!=null) {
                //append trigger entry if absent (corresponding to a given handler)
                Collection<NotificationTrigger> triggers = listTriggers();
                for (TriggerHandler handler : handlers) {
                    if (!alreadyHasCorrespondingTrigger(handler.getTriggerType(), triggers)) {
                        notificationDao.addTrigger(new NotificationTrigger(handler.getTriggerType()));
                    }
                }
            }
            logger.info("Notification manager initialized with [{}] handlers", (handlers!=null? handlers.size() : 0));
        }
    }

    @Override
    public void execute() {
        if (enabled) {
            Collection<Long> triggerKeys = notificationDao.listTriggerKeys();
            if (triggerKeys!=null && !triggerKeys.isEmpty()) {
                for (Long tKey : triggerKeys) {
                    NotificationTrigger trigger = notificationDao.getTrigger(tKey);
                    logger.debug("try to execute notifications on trigger [{}]", trigger);
                    if (trigger != null) {
                        TriggerHandler handler = getHandlerForType(trigger.getType());
                        if (handler != null) {
                            String newState = handler.handleTrigger(trigger.getStoredState(), notificationDao.listTriggerSubscriptions(trigger), trigger.getCfg());
                            trigger.setStoredState(newState);
                            trigger.setChangeDate(new Date());
                            notificationDao.updateTrigger(trigger, tKey);
                        }
                    }
                }
            }
        }
    }

    boolean alreadyHasCorrespondingTrigger(String name, Collection<NotificationTrigger> triggers) {
        boolean result = false;
        if (triggers!=null && !triggers.isEmpty()) {
            for (NotificationTrigger trigger : triggers) {
                if (name.equalsIgnoreCase(trigger.getType())) {
                    return true;
                }
            }
        }
        return result;
    }

    TriggerHandler getHandlerForType(String type) {
        TriggerHandler result = null;
        if (type!=null && handlers!=null) {
            for (TriggerHandler handler : handlers) {
                if (type.equalsIgnoreCase(handler.getTriggerType())) {
                    result = handler;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public Subscription getSubscription(long id) {
        return notificationDao.getSubscription(id);
    }

    @Override
    public NotificationTrigger getTrigger(long id) {
        return notificationDao.getTrigger(id);
    }

    @Override
    public long addSubscription(Subscription subscription) {
        return notificationDao.addSubscription(subscription);
    }

    @Override
    public void removeSubscription(long id) {
        notificationDao.removeSubscription(id);
    }

    @Override
    public long addTrigger(NotificationTrigger trigger) {
        return notificationDao.addTrigger(trigger);
    }

    @Override
    public void updateSubscription(Subscription subscription, long id) {
        notificationDao.updateSubscription(subscription, id);
    }

    @Override
    public Collection<Subscription> listSubscriptions() {
        return notificationDao.listSubscriptions();
    }

    @Override
    public Collection<NotificationTrigger> listTriggers() {
        return notificationDao.listTriggers();
    }

    @Override
    public Collection<TriggerHandler> listHandlers() {
        return handlers;
    }

    @Override
    public GenericPage<Subscription> listSubscriptions(SearchCommand command) {
        return notificationDao.listSubscriptions(command);
    }

}
