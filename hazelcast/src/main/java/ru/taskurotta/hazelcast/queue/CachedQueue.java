package ru.taskurotta.hazelcast.queue;

import com.hazelcast.core.DistributedObject;
import ru.taskurotta.hazelcast.queue.impl.stats.LocalQueueStats;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 */
public interface CachedQueue<E> extends Queue<E>, DistributedObject {

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions. Returns
     * <tt>true</tt> upon success and <tt>false</tt> if no space is currently
     * available.
     *
     * @param e the element to add
     * @return <tt>true</tt> if the element was added to this queue,
     *         <tt>false</tt> otherwise
     */
    @Override
    public boolean offer(E e);

    /**
     * Inserts the specified element into this queue, waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * @param e the element to add
     * @param timeout how long to wait before giving up, in units of
     *        <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determines how to interpret the
     *        <tt>timeout</tt> parameter
     * @return <tt>true</tt> if successful, or <tt>false</tt> if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    public E take() throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue,
     * or returns <tt>null</tt> if this queue is empty.
     *
     * @return the head of this queue, or <tt>null</tt> if this queue is empty
     */
    @Override
    public E poll();

    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *        <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     *        <tt>timeout</tt> parameter
     * @return the head of this queue, or <tt>null</tt> if the
     *         specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Returns the number of elements in this collection.  If this collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this collection
     */
    @Override
    public int size();

    /**
     * Returns LocalQueueStats for this queue.
     * LocalQueueStats is the statistics for the local portion of this
     * queue.
     *
     * @return this queue's local statistics.
     */
    public LocalQueueStats getLocalQueueStats();
}
