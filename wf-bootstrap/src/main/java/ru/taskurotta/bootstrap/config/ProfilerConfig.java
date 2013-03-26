package ru.taskurotta.bootstrap.config;

import ru.taskurotta.bootstrap.profiler.Profiler;

/**
 * User: stukushin
 * Date: 26.03.13
 * Time: 11:24
 */
public interface ProfilerConfig {

    public Profiler getProfiler(Class actorInterface);
}
