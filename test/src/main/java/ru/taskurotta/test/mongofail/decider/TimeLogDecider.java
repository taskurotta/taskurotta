package ru.taskurotta.test.mongofail.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * Date: 18.02.14 15:46
 */
@Decider
public interface TimeLogDecider {

    @Execute
    public void execute();

}
