package ru.taskurotta.hazelcast.queue.delay;

import ru.taskurotta.hazelcast.queue.CachedQueue;

import java.util.concurrent.TimeUnit;

public interface CachedDelayQueue<E> extends CachedQueue<E> {

    /**
     * Inserts the specified element into this queue, waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * @param e         the element to add
     * @param delayTime how long to wait before offer element to the queue
     * @param unit      a <tt>TimeUnit</tt> determines how to interpret the
     *                  <tt>timeout</tt> parameter
     * @return <tt>true</tt> if successful, or <tt>false</tt> if
     * the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     */
    boolean delayOffer(E e, long delayTime, TimeUnit unit) throws InterruptedException;

    long delaySize();
}
