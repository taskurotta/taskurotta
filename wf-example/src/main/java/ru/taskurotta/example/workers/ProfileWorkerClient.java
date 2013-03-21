package ru.taskurotta.example.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.example.Profile;

/**
 * User: stukushin
 * Date: 07.02.13
 * Time: 13:17
 */
@WorkerClient(worker = ProfileWorker.class)
public interface ProfileWorkerClient {

    public Promise<Profile> getUserProfile(long userId);
    public void blockNotification(long userId);;
}
