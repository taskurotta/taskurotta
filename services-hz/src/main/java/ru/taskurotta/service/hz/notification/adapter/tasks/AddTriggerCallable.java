package ru.taskurotta.service.hz.notification.adapter.tasks;

import ru.taskurotta.service.hz.notification.adapter.HzNotificationDaoAdapter;
import ru.taskurotta.service.notification.model.NotificationTrigger;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created on 15.06.2015.
 */
public class AddTriggerCallable implements Callable<Long>, Serializable {

    private NotificationTrigger notificationTrigger;

    public AddTriggerCallable(NotificationTrigger notificationTrigger) {
        this.notificationTrigger = notificationTrigger;
    }

    @Override
    public Long call() throws Exception {
        return HzNotificationDaoAdapter.getRealNotificationsDao().addTrigger(notificationTrigger);
    }

}
