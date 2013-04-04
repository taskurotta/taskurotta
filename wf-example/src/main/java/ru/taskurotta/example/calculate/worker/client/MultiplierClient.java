package ru.taskurotta.example.calculate.worker.client;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.example.calculate.worker.Multiplier;

@WorkerClient(worker = Multiplier.class)
public interface MultiplierClient {

    public Promise<Integer> multiply(Integer a, Integer b);

}
