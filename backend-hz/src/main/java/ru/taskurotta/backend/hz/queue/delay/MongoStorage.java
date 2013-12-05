package ru.taskurotta.backend.hz.queue.delay;

import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:35 AM
 */
public class MongoStorage implements Storage {
    @Override
    public boolean add(Object o, int delayTime, TimeUnit unit) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
