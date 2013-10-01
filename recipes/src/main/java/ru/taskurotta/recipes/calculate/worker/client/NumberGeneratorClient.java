package ru.taskurotta.recipes.calculate.worker.client;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.calculate.worker.NumberGenerator;

@WorkerClient(worker = NumberGenerator.class)
public interface NumberGeneratorClient {

    public Promise<Integer> getNumber();

}
