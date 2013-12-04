package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemListener;
import com.hazelcast.monitor.LocalQueueStats;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * TODO: implement all methods as proxy to call original queue.
 */
public class DelayIQueue<E> implements IQueue<E> {

    private final Storage storage;
    private IQueue queue;

    protected DelayIQueue(IQueue queue, Storage storage) {
        this.queue = queue;
        this.storage = storage;
    }

    @Override
    public boolean add(E e) {
        return false;
    }

    public boolean add(E e, int delayTime, TimeUnit unit) {
        if (delayTime == 0) {
            return add(e);
        }

        return storage.add(e, delayTime, unit);
    }

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public void put(E e) throws InterruptedException {

    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public E take() throws InterruptedException {
        return null;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return 0;
    }

    @Override
    public E remove() {
        return null;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E element() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public LocalQueueStats getLocalQueueStats() {
        return null;
    }

    @Override
    public String addItemListener(ItemListener<E> listener, boolean includeValue) {
        return null;
    }

    @Override
    public boolean removeItemListener(String registrationId) {
        return false;
    }

    @Override
    public Object getId() {
        return null;
    }

    @Override
    public String getPartitionKey() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
