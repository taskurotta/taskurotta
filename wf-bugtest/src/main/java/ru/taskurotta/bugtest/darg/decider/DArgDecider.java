package ru.taskurotta.bugtest.darg.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

@Decider
public interface DArgDecider {

    @Execute
    public void start();

}
