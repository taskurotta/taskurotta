package ru.taskurotta.internal.core;

import ru.taskurotta.core.ArgExtractor;

/**
 */
public class ProxyArgExtractor implements ArgExtractor {

    private Object arg;

    public ProxyArgExtractor(Object arg) {
        this.arg = arg;
    }

    @Override
    public Object get(Class clazz) {
        return arg;
    }
}
