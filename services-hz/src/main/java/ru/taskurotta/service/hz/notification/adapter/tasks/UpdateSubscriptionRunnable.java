package ru.taskurotta.service.hz.notification.adapter.tasks;

import ru.taskurotta.service.hz.notification.adapter.HzNotificationDaoAdapter;
import ru.taskurotta.service.notification.model.Subscription;

import java.io.Serializable;

/**
 * Created on 15.06.2015.
 */
public class UpdateSubscriptionRunnable implements Runnable, Serializable {

    private Subscription subscription;
    private long id;

    public UpdateSubscriptionRunnable(Subscription subscription, long id) {
        this.subscription = subscription;
        this.id = id;
    }

    @Override
    public void run() {
        HzNotificationDaoAdapter.getRealNotificationsDao().updateSubscription(subscription, id);
    }
}
