package ru.taskurotta.bugtest.darg.decider;

import ru.taskurotta.annotation.DeciderClient;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 15.07.13 17:46
 */
@DeciderClient(decider = DArgDecider.class)
public interface DArgDeciderClient {

    public void start(String param);

}
