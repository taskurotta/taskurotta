package ru.taskurotta.hazelcast.queue.delay.impl;

import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;
import ru.taskurotta.hazelcast.queue.delay.CachedDelayQueue;
import ru.taskurotta.hazelcast.queue.delay.Storage;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class CachedDelayQueueImpl<E> implements CachedDelayQueue<E> {

    private final Storage<E> storage;
    private CachedQueue<E> queue;

    public CachedDelayQueueImpl(CachedQueue<E> queue, Storage<E> storage) {
        this.queue = queue;
        this.storage = storage;
    }

    @Override
    public boolean delayOffer(E e, long delayTime, TimeUnit unit) {
        return delayTime > 0 ? storage.add(e, delayTime, unit) : offer(e);
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        return queue.offer(e) || storage.add(e, 0l, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return queue.offer(e, timeout, unit);
    }

    @Override
    public E take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o) || storage.remove((E) o);
    }

    @Override
    public boolean contains(Object o) {
        throw new IllegalStateException("Not implemented!!!");
    }

    @Override
    public E remove() {
        return queue.remove();
    }

    @Override
    public E poll() {
        return queue.poll();
    }

    @Override
    public E element() {
        return queue.element();
    }

    @Override
    public E peek() {
        return queue.peek();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return queue.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public void clear() {
        storage.clear();
        queue.clear();
    }

    @Override
    public LocalCachedQueueStats getLocalQueueStats() {
        return queue.getLocalQueueStats();
    }

    @Override
    public Object getId() {
        return queue.getId();
    }

    @Override
    public String getPartitionKey() {
        return queue.getPartitionKey();
    }

    @Override
    public String getName() {
        return queue.getName();
    }

    @Override
    public String getServiceName() {
        return queue.getServiceName();
    }

    @Override
    public void destroy() {
        storage.destroy();
        queue.destroy();
    }

}
