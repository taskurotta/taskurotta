package ru.taskurotta.backend.hz.queue.delay;

import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:35 AM
 */
public class MongoStorage implements Storage {
    @Override
    public boolean add(Object o, long delayTime, TimeUnit unit) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
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
    public void clear() {

    }

    @Override
    public void destroy() {

    }

}
