package ru.taskurotta.bootstrap.config;

import ru.taskurotta.RuntimeProcessor;

/**
 * User: stukushin
 * Date: 04.02.13
 * Time: 16:50
 */
public interface RuntimeConfig {

    public void init();

    public RuntimeProcessor getRuntimeProcessor(Class actorInterface);

}