/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.taskurotta.hazelcast.queue.impl.proxy;

import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.InitializingObject;
import com.hazelcast.spi.NodeEngine;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;
import ru.taskurotta.hazelcast.queue.impl.QueueService;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Proxy implementation for the Queue.
 *
 * @param <E>
 */
public class QueueProxyImpl<E> extends QueueProxySupport implements CachedQueue<E>, InitializingObject {

    public QueueProxyImpl(String name, QueueService queueService, NodeEngine nodeEngine) {
        super(name, queueService, nodeEngine);
    }

    @Override
    public LocalCachedQueueStats getLocalQueueStats() {
        return getService().createLocalQueueStats(name, partitionId);
    }

    @Override
    public boolean add(E e) {
        if (offer(e)) {
            return true;
        }
        throw new IllegalStateException("Queue is full!");
    }

    @Override
    public boolean remove(Object o) {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public boolean offer(E e) {
        try {
            return offer(e, 0, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            return false;
        }
    }

    @Override
    public E remove() {
        throw new IllegalStateException("Not implemented yet!");
    }


    @Override
    public boolean offer(E e, long timeout, TimeUnit timeUnit) throws InterruptedException {
        final NodeEngine nodeEngine = getNodeEngine();
        final Data data = nodeEngine.toData(e);
        return offerInternal(data, timeUnit.toMillis(timeout));
    }

    @Override
    public E take() throws InterruptedException {
        return poll(-1, TimeUnit.MILLISECONDS);
    }

    @Override
    public E poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
        final NodeEngine nodeEngine = getNodeEngine();
        final Object data = pollInternal(timeUnit.toMillis(timeout));
        return nodeEngine.toObject(data);
    }


    @Override
    public E poll() {
        try {
            return poll(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //todo: interrupt status is lost
            return null;
        }
    }

    @Override
    public E element() {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public E peek() {
        throw new IllegalStateException("Not implemented yet!");
    }


    @Override
    public boolean contains(Object o) {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public Iterator<E> iterator() {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public Object[] toArray() {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new IllegalStateException("Not implemented yet!");
    }


    @Override
    public boolean addAll(Collection<? extends E> es) {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CachedQueue");
        sb.append("{name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
