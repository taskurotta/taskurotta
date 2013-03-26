package ru.taskurotta.bootstrap.config;

import ru.taskurotta.client.TaskSpreader;

/**
 * User: romario
 * Date: 2/12/13
 * Time: 5:17 PM
 */
public interface SpreaderConfig {

    public void init();

    public TaskSpreader getTaskSpreader(Class clazz);
}
