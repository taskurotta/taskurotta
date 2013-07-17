package ru.taskurotta.recipes.darg.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.core.Promise;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 17.07.13 16:12
 */
@DeciderClient(decider = DArgSubprocessDecider.class)
public interface DArgSubprocessDeciderClient {

    public Promise<String> getSubprocessValue(Promise<String> someParam);

}
