package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.IMap;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:35 AM
 */
public class BaseStorage implements Storage {

    private IMap storage;

    public BaseStorage(IMap storage) {
        this.storage = storage;
    }

    @Override
    public boolean add(Object o, int delayTime, TimeUnit unit) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection getReadyItems() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
