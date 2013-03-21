package ru.taskurotta.client;

/**
 * User: romario
 * Date: 2/19/13
 * Time: 1:50 PM
 */
public interface ClientServiceManager {

    /**
     * @return
     */
    public DeciderClientProvider getDeciderClientProvider();

    /**
     * @return
     */
    public TaskSpreaderProvider getTaskSpreaderProvider();

}
