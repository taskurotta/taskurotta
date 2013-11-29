package ru.taskurotta.recipes.notification.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 07.02.13
 * Time: 13:13
 */
@Decider
public interface NotificationDecider {

    @Execute
    public void sendMessage(long userId, String message);
}
