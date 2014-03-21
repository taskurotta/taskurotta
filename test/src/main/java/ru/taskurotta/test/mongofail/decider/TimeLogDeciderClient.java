package ru.taskurotta.test.mongofail.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.core.ActorSchedulingOptions;

/**
 * Date: 19.02.14 13:12
 */
@DeciderClient(decider = TimeLogDecider.class)
public interface TimeLogDeciderClient {

    public void execute(ActorSchedulingOptions opt);

}
