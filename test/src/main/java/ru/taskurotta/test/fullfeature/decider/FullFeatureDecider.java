package ru.taskurotta.test.fullfeature.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * Created by void 20.12.13 17:53
 */
@Decider
public interface FullFeatureDecider {

    @Execute
    public void start();
}
