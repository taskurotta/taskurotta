package ru.taskurotta.recipes.retrypolicy.decider;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.retrypolicy.workers.SumWorkerClient;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:06
 */
public class SumDeciderImpl implements SumDecider {
    private SumWorkerClient sumWorker;
    private SumDeciderImpl asynchronous;

    @Override
    public void calculate(int a, int b) {
        Promise<Integer> sum = sumWorker.sum(a, b);
        asynchronous.show(a, b, sum);
    }

    @Asynchronous
    public void show(int a, int b, Promise<Integer> sum) {
        System.out.println(a + " + " + b + " = " + sum.get());
        System.exit(0);
    }

    public void setSumWorker(SumWorkerClient sumWorker) {
        this.sumWorker = sumWorker;
    }

    public void setAsynchronous(SumDeciderImpl asynchronous) {
        this.asynchronous = asynchronous;
    }
}
