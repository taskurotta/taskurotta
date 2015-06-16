package ru.taskurotta.test.fullfeature.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * Created by void 20.12.13 17:54
 */
@DeciderClient(decider = FullFeatureDecider.class)
public interface FullFeatureDeciderClient {

    @Execute
    void start();
}
