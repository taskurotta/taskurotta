package ru.taskurotta.recipes.retrypolicy.workers;

import ru.taskurotta.annotation.ExponentialRetry;
import ru.taskurotta.annotation.LinearRetry;
import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:03
 */
@Worker
public interface SumWorker {
//    @LinearRetry(initialRetryIntervalSeconds = 5)
@ExponentialRetry(
        initialRetryIntervalSeconds = 5,
        maximumRetryIntervalSeconds = 30,
        retryExpirationSeconds = 120, // неделя
        exceptionsToRetry = {
                RuntimeException.class
        }
)public int sum(int a, int b);
}
