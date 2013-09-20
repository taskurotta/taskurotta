package ru.taskurotta.recipes.calculate.worker;

import ru.taskurotta.annotation.LinearRetry;
import ru.taskurotta.annotation.Worker;

@Worker
public interface NumberGenerator {

    @LinearRetry(initialRetryIntervalSeconds=5)
    public Integer getNumber();

}
