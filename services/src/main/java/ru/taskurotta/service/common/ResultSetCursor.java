package ru.taskurotta.service.common;

import java.io.Closeable;
import java.util.Collection;

/**
 */
public interface ResultSetCursor<T> extends Closeable {

    public Collection<T> getNext();
}
