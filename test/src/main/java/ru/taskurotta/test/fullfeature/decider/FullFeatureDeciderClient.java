package ru.taskurotta.test.fullfeature.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.TaskConfig;

/**
 * Created by void 20.12.13 17:54
 */
@DeciderClient(decider = FullFeatureDecider.class)
public interface FullFeatureDeciderClient {

    @Execute
    public void start();

    @Execute
    public void start(TaskConfig taskConfig);
}
