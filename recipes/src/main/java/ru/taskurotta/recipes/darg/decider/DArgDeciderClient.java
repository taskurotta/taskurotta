package ru.taskurotta.recipes.darg.decider;

import ru.taskurotta.annotation.DeciderClient;

@DeciderClient(decider = DArgDecider.class)
public interface DArgDeciderClient {

    public void start(String inputParam);

}
