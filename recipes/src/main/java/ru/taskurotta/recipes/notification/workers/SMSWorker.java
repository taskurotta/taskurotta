package ru.taskurotta.recipes.notification.workers;

import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 15:27
 */
@Worker
public interface SMSWorker {
    public boolean send(String phoneNumber, String message);
}
