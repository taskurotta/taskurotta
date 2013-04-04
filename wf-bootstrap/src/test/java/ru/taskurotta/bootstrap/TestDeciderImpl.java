package ru.taskurotta.bootstrap;

import org.junit.Ignore;

/**
 * User: stukushin
 * Date: 02.04.13
 * Time: 19:38
 */
@Ignore
public class TestDeciderImpl implements TestDecider {
    private TestWorkerClient testWorker;

    @Override
    public void start(int a, int b) {
        testWorker.sum(a, b);
    }

    public void setTestWorker(TestWorkerClient testWorker) {
        this.testWorker = testWorker;
    }
}
