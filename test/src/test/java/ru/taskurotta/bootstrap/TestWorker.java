package ru.taskurotta.bootstrap;

import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 18:31
 */

@Worker
public interface TestWorker {
    public int sum(int a, int b);
}
