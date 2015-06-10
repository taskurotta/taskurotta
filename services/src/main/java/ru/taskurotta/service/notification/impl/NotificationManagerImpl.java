package ru.taskurotta.service.notification.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.service.notification.NotificationManager;
import ru.taskurotta.service.notification.TriggerHandler;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.Subscription;
import ru.taskurotta.service.storage.EntityStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Created on 08.06.2015.
 */
public class NotificationManagerImpl implements NotificationManager {

    private static final Logger logger = LoggerFactory.getLogger(NotificationManagerImpl.class);

    private EntityStore<Subscription> subscriptionsStore;
    private EntityStore<NotificationTrigger> triggersStore;
    private Collection<TriggerHandler> handlers;

    @Override
    public void execute() {
        Collection<Long> triggerKeys = triggersStore.getKeys();
        if (triggerKeys!=null && triggerKeys.isEmpty()) {
            for (Long tKey : triggerKeys) {
                NotificationTrigger trigger = triggersStore.get(tKey);
                if (trigger != null) {
                    TriggerHandler handler = getHandlerForType(trigger.getType());
                    if (handler != null) {
                        String newState = handler.handleTrigger(trigger.getStoredState(), listTriggerSubscriptions(trigger), trigger.getCfg());
                        trigger.setStoredState(newState);
                        trigger.setChangeDate(new Date());
                        triggersStore.update(trigger, tKey);
                    }
                }
            }
        }
    }

    TriggerHandler getHandlerForType(String type) {
        TriggerHandler result = null;
        if (type!=null && handlers!=null) {
            for (TriggerHandler handler : handlers) {
                if (type.equalsIgnoreCase(handler.getName())) {
                    result = handler;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public Subscription getSubscription(long id) {
        return subscriptionsStore.get(id);
    }

    @Override
    public long addSubscription(Subscription cfg) {
        return subscriptionsStore.add(cfg);
    }

    @Override
    public void updateSubscription(Subscription cfg, long id) {
        subscriptionsStore.update(cfg, id);
    }

    @Override
    public Collection<Subscription> listSubscriptions() {
        return subscriptionsStore.getAll();
    }

    @Override
    public Collection<NotificationTrigger> listTriggers() {
        return triggersStore.getAll();
    }

    @Override
    public Collection<Subscription> listTriggerSubscriptions(NotificationTrigger trigger) {
        Collection<Subscription> result = new ArrayList<>();
        Collection<Long> sKeys = subscriptionsStore.getKeys();
        if (sKeys!=null) {
            for (Long key : sKeys) {
                Subscription s = subscriptionsStore.get(key);
                if (s!=null && s.getTriggersKeys()!=null && s.getTriggersKeys().contains(trigger.getId())) {
                    result.add(s);
                }
            }
        }
        return result.isEmpty()? null : result;
    }


    @Required
    public void setSubscriptionsStore(EntityStore<Subscription> subscriptionsStore) {
        this.subscriptionsStore = subscriptionsStore;
    }

    @Required
    public void setHandlers(Collection<TriggerHandler> handlers) {
        this.handlers = handlers;
    }

    @Required
    public void setTriggersStore(EntityStore<NotificationTrigger> triggersStore) {
        this.triggersStore = triggersStore;
    }
}
