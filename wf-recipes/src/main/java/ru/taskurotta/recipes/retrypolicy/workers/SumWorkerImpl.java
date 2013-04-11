package ru.taskurotta.recipes.retrypolicy.workers;

import java.util.Random;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:05
 */
public class SumWorkerImpl implements SumWorker {

    private int numberOfTries;
    private int maxNumberOfTries = 4;

    @Override
    public int sum(int a, int b) {

//        Random random = new Random();
//        if (random.nextInt(10) > 5) {
//            throw new RuntimeException("Test exception");
//        }


        if (numberOfTries < maxNumberOfTries) {
            numberOfTries++;
            throw new RuntimeException("Test exception");
        }

        return a + b;
    }
}
