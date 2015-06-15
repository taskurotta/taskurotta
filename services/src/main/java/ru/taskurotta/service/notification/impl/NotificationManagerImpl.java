package ru.taskurotta.service.notification.impl;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.service.notification.NotificationManager;
import ru.taskurotta.service.notification.TriggerHandler;
import ru.taskurotta.service.notification.dao.NotificationDao;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.Subscription;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created on 08.06.2015.
 */
public class NotificationManagerImpl implements NotificationManager, ApplicationContextAware {

    private NotificationDao notificationDao;

    private Collection<TriggerHandler> handlers;

    private ApplicationContext applicationContext;

    public NotificationManagerImpl(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    public void init() {
        Map<String, TriggerHandler> resultMap = applicationContext.getBeansOfType(TriggerHandler.class);
        if (resultMap!=null && !resultMap.isEmpty()) {
            this.handlers = resultMap.values();

            //append trigger entry if absent (corresponding to a given handler)
            Collection<NotificationTrigger> triggers = listTriggers();
            for (TriggerHandler handler : handlers) {
                if (!alreadyHasCorrespondingTrigger(handler.getTriggerType(), triggers)) {
                    notificationDao.addTrigger(new NotificationTrigger(handler.getTriggerType()));
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

    @Override
    public void execute() {
        Collection<Long> triggerKeys = notificationDao.listTriggerKeys();
        if (triggerKeys!=null && triggerKeys.isEmpty()) {
            for (Long tKey : triggerKeys) {
                NotificationTrigger trigger = notificationDao.getTrigger(tKey);
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
