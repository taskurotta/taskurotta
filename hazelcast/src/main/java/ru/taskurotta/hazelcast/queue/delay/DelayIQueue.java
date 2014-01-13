package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemListener;
import com.hazelcast.monitor.LocalQueueStats;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class DelayIQueue<E> implements IQueue<E> {

    private final Storage<E> storage;
    private IQueue<E> queue;

    protected DelayIQueue(IQueue<E> queue, Storage<E> storage) {
        this.queue = queue;
        this.storage = storage;
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    public boolean add(E e, long delayTime, TimeUnit unit) {
        if (delayTime == 0) {
            return offer(e);
        }

        return storage.add(e, delayTime, unit);
    }

    @Override
    public boolean offer(E e) {
        return queue.offer(e) || storage.add(e, 0l, TimeUnit.MILLISECONDS);
    }

    @Override
    public void put(E e) throws InterruptedException {
        queue.put(e);
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
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o) || storage.remove((E) o);
    }

    @Override
    public boolean contains(Object o) {
        throw new NotImplementedException();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return queue.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return queue.drainTo(c, maxElements);
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
    public LocalQueueStats getLocalQueueStats() {
        return queue.getLocalQueueStats();
    }

    @Override
    public String addItemListener(ItemListener<E> listener, boolean includeValue) {
        return queue.addItemListener(listener, includeValue);
    }

    @Override
    public boolean removeItemListener(String registrationId) {
        return queue.removeItemListener(registrationId);
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
