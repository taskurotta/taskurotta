package ru.taskurotta.service.hz.adapter.notification.tasks;

import ru.taskurotta.service.hz.adapter.notification.HzNotificationDaoAdapter;
import ru.taskurotta.service.notification.model.Subscription;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created on 15.06.2015.
 */
public class AddSubscriptionCallable implements Callable<Long>, Serializable {

    private Subscription subscription;
    public AddSubscriptionCallable(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public Long call() throws Exception {
        return HzNotificationDaoAdapter.getRealNotificationsDao().addSubscription(subscription);
    }
}
