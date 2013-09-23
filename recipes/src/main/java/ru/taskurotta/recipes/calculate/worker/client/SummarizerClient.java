package ru.taskurotta.recipes.calculate.worker.client;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.calculate.worker.Summarizer;

@WorkerClient(worker = Summarizer.class)
public interface SummarizerClient {

    public Promise<Integer> summarize(Integer a, Integer b);

}
