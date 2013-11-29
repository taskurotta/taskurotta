package ru.taskurotta.recipes.parallel.workers;

/**
 * User: stukushin
 * Date: 18.03.13
 * Time: 14:47
 */
public class PiWorkerImpl implements PiWorker {

    @Override
    public double calculate(long start, long elements) {
        double result = 0;

        for (long i = start * elements; i <= ((start + 1) * elements - 1); i++) {
            result += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
        }

        return result;
    }
}
