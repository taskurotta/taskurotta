package ru.taskurotta.bootstrap;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 02.04.13
 * Time: 19:37
 */
@DeciderClient(decider = TestDecider.class)
public interface TestDeciderClient {
    @Execute
    public void start(int a, int b);
}
