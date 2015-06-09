package ru.taskurotta.service.notification;

import ru.taskurotta.service.notification.model.NotificationConfig;

import java.util.List;

/**
 * Created on 08.06.2015.
 */
public interface NotificationManager {

    NotificationConfig getConfig(long id);

    long addConfig(NotificationConfig cfg);

    void updateConfig(NotificationConfig cfg, long id);

    List<NotificationConfig> getConfigs();

}
