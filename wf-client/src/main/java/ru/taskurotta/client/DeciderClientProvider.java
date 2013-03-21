package ru.taskurotta.client;

import ru.taskurotta.core.Task;

/**
 * User: romario
 * Date: 2/19/13
 * Time: 1:50 PM
 */
public interface DeciderClientProvider {

    /**
     * get cached decider client proxy object
     *
     * @param type - interface of required object
     * @return proxy object or null
     */
    public <DeciderClientType> DeciderClientType getDeciderClient(Class<DeciderClientType> type);


    /**
     * @param task
     */
    public void startProcess(Task task);

}
