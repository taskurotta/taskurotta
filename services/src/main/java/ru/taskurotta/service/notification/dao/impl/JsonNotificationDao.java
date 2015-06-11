package ru.taskurotta.service.notification.dao.impl;

import ru.taskurotta.service.notification.dao.NotificationDao;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.Subscription;
import ru.taskurotta.service.storage.EntityStore;
import ru.taskurotta.service.storage.impl.JsonEntityStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created on 11.06.2015.
 */
public class JsonNotificationDao implements NotificationDao {

    public static final String DIR_SUBSCRIPTIONS = "subscriptions";
    public static final String DIR_TRIGGERS = "triggers";

    private EntityStore<Subscription> subscriptionsStore;
    private EntityStore<NotificationTrigger> triggersStore;

    public JsonNotificationDao(String storeLocation) {
        subscriptionsStore = new JsonEntityStore<>(Subscription.class, storeLocation + File.separator + DIR_SUBSCRIPTIONS);
        triggersStore = new JsonEntityStore<>(NotificationTrigger.class, storeLocation + File.separator + DIR_TRIGGERS);
    }

    @Override
    public Subscription getSubscription(long id) {
        Subscription res = subscriptionsStore.get(id);
        if (res!=null) {
            res.setId(id);
        }
        return res;
    }

    @Override
    public NotificationTrigger getTrigger(long id) {
        NotificationTrigger res = triggersStore.get(id);
        if (res!=null) {
            res.setId(id);
        }
        return res;
    }

    @Override
    public long addSubscription(Subscription subscription) {
        return subscriptionsStore.add(subscription);
    }

    @Override
    public long addTrigger(NotificationTrigger trigger) {
        return triggersStore.add(trigger);
    }

    @Override
    public void updateSubscription(Subscription subscription, long id) {
        subscriptionsStore.update(subscription, id);
    }

    @Override
    public void updateTrigger(NotificationTrigger trigger, long id) {
        triggersStore.update(trigger, id);
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

    @Override
    public Collection<Long> listTriggerKeys() {
        return triggersStore.getKeys();
    }
}
