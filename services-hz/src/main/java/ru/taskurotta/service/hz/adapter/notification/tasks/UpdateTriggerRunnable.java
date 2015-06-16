package ru.taskurotta.service.hz.adapter.notification.tasks;

import ru.taskurotta.service.hz.adapter.notification.HzNotificationDaoAdapter;
import ru.taskurotta.service.notification.model.NotificationTrigger;

import java.io.Serializable;

/**
 * Created on 15.06.2015.
 */
public class UpdateTriggerRunnable implements Runnable, Serializable {

    private NotificationTrigger notificationTrigger;
    private long id;

    public UpdateTriggerRunnable(NotificationTrigger notificationTrigger, long id) {
        this.notificationTrigger = notificationTrigger;
        this.id = id;
    }

    @Override
    public void run() {
        HzNotificationDaoAdapter.getRealNotificationsDao().updateTrigger(notificationTrigger, id);
    }
}
