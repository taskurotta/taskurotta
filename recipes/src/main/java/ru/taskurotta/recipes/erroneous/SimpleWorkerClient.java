package ru.taskurotta.recipes.erroneous;

import ru.taskurotta.annotation.AcceptFail;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

/**
 * Created by void 18.10.13 18:28
 */
@WorkerClient(worker = SimpleWorker.class)
public interface SimpleWorkerClient {

    @AcceptFail(type = RuntimeException.class)
    Promise<Integer> createNumber();

    Promise<Integer> print(Promise<Integer> number);
}
