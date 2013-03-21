package ru.taskurotta.core;

import java.util.UUID;

/**
 * User: romario
 * Date: 12/26/12
 * Time: 12:07 PM
 */
public interface TaskDecision {

    /**
     * @return Task unique Id
     */
    public UUID getId();


    /**
     * @return retirned value
     */
    Object getValue();


    /**
     * TODO: to Array
     * @return produced tasks
     */
    Task[] getTasks();
}
