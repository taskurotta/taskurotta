package ru.taskurotta.fir.client.deciders;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 11:18
 */

@DeciderClient(decider = FirDecider.class)
public interface FirDeciderClient {
    @Execute
    public void start(String uuid, int a, int b);
}
