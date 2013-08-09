package ru.taskurotta.restarter.test.deciders;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 17:50
 */
@DeciderClient(decider = Coordinator.class)
public interface CoordinatorClient {
    @Execute
    public void start();
}
