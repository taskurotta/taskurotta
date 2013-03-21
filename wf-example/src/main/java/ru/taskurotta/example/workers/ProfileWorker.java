package ru.taskurotta.example.workers;

import ru.taskurotta.annotation.Worker;
import ru.taskurotta.example.Profile;

/**
 * User: stukushin
 * Date: 07.02.13
 * Time: 13:14
 */
@Worker
public interface ProfileWorker {

    public Profile getUserProfile(long userId);

    public void blockNotification(long userId);
}
