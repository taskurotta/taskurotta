package ru.taskurotta.recipes.notification.workers;

import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 15:38
 */
@Worker
public interface EmailWorker {
    public boolean send(String email, String message);
}
