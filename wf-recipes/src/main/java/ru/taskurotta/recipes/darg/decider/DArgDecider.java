package ru.taskurotta.recipes.darg.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

@Decider
public interface DArgDecider {

    @Execute
    public void start(String inputParam);

}
