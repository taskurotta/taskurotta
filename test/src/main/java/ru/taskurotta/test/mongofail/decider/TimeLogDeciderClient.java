package ru.taskurotta.test.mongofail.decider;

import ru.taskurotta.annotation.DeciderClient;

/**
 * Date: 19.02.14 13:12
 */
@DeciderClient(decider = TimeLogDecider.class)
public interface TimeLogDeciderClient {

    public void execute();

}
