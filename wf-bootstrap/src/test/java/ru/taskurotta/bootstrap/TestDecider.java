package ru.taskurotta.bootstrap;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 02.04.13
 * Time: 19:37
 */
@Decider
public interface TestDecider {
    @Execute
    public void start(int a, int b);
}
