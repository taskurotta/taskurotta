package ru.taskurotta.recipes.retrypolicy.workers;

import ru.taskurotta.annotation.LinearRetry;
import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:03
 */
@Worker
public interface SumWorker {
    @LinearRetry(initialRetryIntervalSeconds = 5)
    public int sum(int a, int b);
}
