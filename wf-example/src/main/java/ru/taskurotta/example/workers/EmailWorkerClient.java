package ru.taskurotta.example.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 15:42
 */
@WorkerClient(worker = EmailWorker.class)
public interface EmailWorkerClient {
    public Promise<Boolean> send(String email, String message);
}
