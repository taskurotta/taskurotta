package ru.taskurotta.recipes.scheduled;

import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.Worker;

/**
 * User: dimadin
 * Date: 26.09.13 16:17
 */
@Worker
public interface TimeLogger {

    @Execute
    public void log(String arg1, String arg2, int arg3);

}
