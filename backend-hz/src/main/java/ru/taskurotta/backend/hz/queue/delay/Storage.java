package ru.taskurotta.backend.hz.queue.delay;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 8:55 AM
 */
public interface Storage<E> {

    public boolean add(E e, int delayTime, TimeUnit unit);

    public Collection<E> getReadyItems();
}
