package ru.taskurotta.recipes.retrypolicy.workers;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:05
 */
public class SumWorkerImpl implements SumWorker {

    private int numberOfTries;

    @Override
    public int sum(int a, int b) {
        int maxNumberOfTries = 4;
        if (numberOfTries < maxNumberOfTries) {
            numberOfTries++;
            new RuntimeException("Test exception2").printStackTrace();
            throw new RuntimeException("Test exception2");
        }

        return a + b;
    }
}
