package ru.taskurotta.bootstrap;

import org.junit.Ignore;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 18:33
 */

@Ignore
public class TestWorkerImpl implements TestWorker {
    @Override
    public int sum(int a, int b) {
        return a + b;
    }
}
