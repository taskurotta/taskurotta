package ru.taskurotta.core;

/**
 */
public interface ArgExtractor {

    public<T> T get(Class<T> resultClass);
}
