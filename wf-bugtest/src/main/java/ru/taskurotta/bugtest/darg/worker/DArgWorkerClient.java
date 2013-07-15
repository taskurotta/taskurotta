package ru.taskurotta.bugtest.darg.worker;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

@WorkerClient(worker=DArgWorker.class)
public interface DArgWorkerClient {

    public Promise<Integer> getNumber(Promise<String> param);

}
