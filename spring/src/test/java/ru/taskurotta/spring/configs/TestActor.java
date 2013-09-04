package ru.taskurotta.spring.configs;

import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 13:57
 */

@Worker
public interface TestActor {
    public int sum(int a, int b);
}
