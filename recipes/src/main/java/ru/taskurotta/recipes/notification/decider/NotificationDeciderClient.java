package ru.taskurotta.recipes.notification.decider;

import ru.taskurotta.annotation.DeciderClient;

/**
 * User: romario
 * Date: 2/20/13
 * Time: 10:31 AM
 */
@DeciderClient(decider = NotificationDecider.class)
public interface NotificationDeciderClient {

    public void sendMessage(long userId, String message);

}
