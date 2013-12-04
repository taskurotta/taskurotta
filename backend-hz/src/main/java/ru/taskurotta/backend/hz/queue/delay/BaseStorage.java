package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.IMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:35 AM
 */
public class BaseStorage implements Storage {

    private IMap<Long, Set<Object>> storage;

    private final ReentrantLock lock = new ReentrantLock();

    public BaseStorage(IMap storage) {
        this.storage = storage;
    }

    @Override
    public boolean add(Object o, int delayTime, TimeUnit unit) {

        long delayMilliseconds = TimeUnit.MILLISECONDS.convert(delayTime, unit);

        Set<Object> objects = storage.get(delayMilliseconds);

        if (objects == null) {
            final ReentrantLock lock = this.lock;

            try {
                lock.lock();

                objects = storage.get(delayMilliseconds);
                if (objects == null) {
                    objects = new HashSet<>();
                    storage.put(delayMilliseconds, objects);
                }
            } finally {
                lock.unlock();
            }
        }

        return objects.add(o);
    }

    @Override
    public Collection getReadyItems() {

        Collection<Object> collection = new ArrayList<>();

        long now = System.currentTimeMillis();

        SortedSet<Long> keys = new TreeSet<>(storage.keySet());

        final ReentrantLock lock = this.lock;

        for (Long key : keys) {

            if (key > now) {
                break;
            }

            try {
                lock.lock();

                collection.addAll(storage.remove(key));
            } finally {
                lock.unlock();
            }
        }

        return collection;
    }
}
