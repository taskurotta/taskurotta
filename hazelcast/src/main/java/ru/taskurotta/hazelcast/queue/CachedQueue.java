package ru.taskurotta.hazelcast.queue;

import com.hazelcast.core.BaseQueue;
import com.hazelcast.core.ICollection;
import ru.taskurotta.hazelcast.queue.impl.stats.LocalQueueStats;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 */
public interface CachedQueue<E> extends BlockingQueue<E>, BaseQueue<E>, ICollection<E> {

    /*
     * Added poll(), poll(long timeout, TimeUnit unit) and take()
     * methods here to prevent wrong method return type issue when
     * compiled with java 8.
     *
     * For additional details see:
     *
     * http://mail.openjdk.java.net/pipermail/compiler-dev/2014-November/009139.html
     * https://bugs.openjdk.java.net/browse/JDK-8064803
     *
     */

    E poll();

    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    E take() throws InterruptedException;

    /**
     * Returns LocalQueueStats for this queue.
     * LocalQueueStats is the statistics for the local portion of this
     * queue.
     *
     * @return this queue's local statistics.
     */
    LocalQueueStats getLocalQueueStats();
}
