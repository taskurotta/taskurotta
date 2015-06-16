package ru.taskurotta.service.notification;

import ru.taskurotta.service.notification.model.EmailNotification;

/**
 * Created on 16.06.2015.
 */
public interface EmailSender {

    void send(EmailNotification emailNotification);

}
