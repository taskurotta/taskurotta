package ru.taskurotta.bootstrap;

import java.util.Map;

/**
 * User: stukushin
 * Date: 15.05.13
 * Time: 15:57
 */
public interface BootstrapMBean {

    /**
     * Return current size for every actor pool.
     * @return Map actorId(number of pool), currentSize
     */
    Map<String, Integer> getActorPoolSizes();

    /**
     * Add actor pool with custom poolSize use actor description from config.
     * @param actorId - actorClass#version
     * @param poolSize - pool poolSize
     */
    void startActorPool(String actorId, int poolSize);

    /**
     * Gracefully stop actor pool.
     * @param actorPoolId - actorClass#version[number of pool]
     */
    void stopActorPool(String actorPoolId);

    /**
     * Shutdown instance. Before it, shutdown all actors pools.
     */
    void shutdown();
}
