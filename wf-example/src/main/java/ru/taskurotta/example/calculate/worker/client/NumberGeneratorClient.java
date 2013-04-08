package ru.taskurotta.example.calculate.worker.client;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.example.calculate.worker.NumberGenerator;

@WorkerClient(worker=NumberGenerator.class)
public interface NumberGeneratorClient {

    public Promise<Integer> getNumber();

}
