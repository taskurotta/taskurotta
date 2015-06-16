package ru.taskurotta.service.hz.adapter.notification.tasks;

import ru.taskurotta.service.hz.adapter.notification.HzNotificationDaoAdapter;

import java.io.Serializable;

/**
 * Created on 15.06.2015.
 */
public class RemoveSubscriptionRunnable implements Runnable, Serializable {

    private long id;

    public RemoveSubscriptionRunnable(long id) {
        this.id = id;
    }

    @Override
    public void run() {
        HzNotificationDaoAdapter.getRealNotificationsDao().removeSubscription(id);
    }
}
