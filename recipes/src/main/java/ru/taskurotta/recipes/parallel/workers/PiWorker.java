package ru.taskurotta.recipes.parallel.workers;

import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 18.03.13
 * Time: 14:44
 */
@Worker
public interface PiWorker {
    public double calculate(long start, long elements);
}
