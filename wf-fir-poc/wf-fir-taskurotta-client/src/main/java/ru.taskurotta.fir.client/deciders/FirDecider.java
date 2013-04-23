package ru.taskurotta.fir.client.deciders;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 11:16
 */

@Decider
public interface FirDecider {
    @Execute
    public void start(String uuid, int a, int b);
}
