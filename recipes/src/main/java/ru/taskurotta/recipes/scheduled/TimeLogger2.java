package ru.taskurotta.recipes.scheduled;

import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.Worker;

/**
 * Created on 21.07.2014.
 */
@Worker
public interface TimeLogger2 {

    @Execute
    public void log();//TimeLogger without arguments

}
