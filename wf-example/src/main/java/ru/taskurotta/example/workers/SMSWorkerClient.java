package ru.taskurotta.example.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 15:44
 */
@WorkerClient(worker = SMSWorker.class)
public interface SMSWorkerClient {
    public Promise<Boolean> send(String phoneNumber, String message);
}
