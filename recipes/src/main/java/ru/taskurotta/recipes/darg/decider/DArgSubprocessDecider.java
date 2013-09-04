package ru.taskurotta.recipes.darg.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Promise;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 17.07.13 16:10
 */
@Decider
public interface DArgSubprocessDecider {

    @Execute
    public Promise<String> getSubprocessValue(String someParam);

}
