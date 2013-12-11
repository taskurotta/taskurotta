package ru.taskurotta.hazelcast.delay;

import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 8:55 AM
 */
public interface Storage<E> {

    public boolean add(E e, long delayTime, TimeUnit unit);

    public boolean remove(E e);

    public void clear();

    public void destroy();
}
