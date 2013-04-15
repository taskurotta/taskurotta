package ru.taskurotta.example.calculate.worker;

import ru.taskurotta.annotation.LinearRetry;
import ru.taskurotta.annotation.Worker;

@Worker
public interface Summarizer {

    @LinearRetry(initialRetryIntervalSeconds=5)
    public Integer summarize(Integer a, Integer b);
}
