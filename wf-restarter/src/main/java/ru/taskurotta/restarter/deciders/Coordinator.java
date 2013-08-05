package ru.taskurotta.restarter.deciders;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 17:49
 */
@Decider
public interface Coordinator {
    @Execute
    public void start();
}
