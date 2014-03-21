package ru.taskurotta.hz.test.queue;

import com.hazelcast.core.QueueStore;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: stukushin
 * Date: 17.01.14
 * Time: 12:40
 */
public class FakeQueueStore implements QueueStore<Integer> {
    @Override
    public void store(Long key, Integer value) {

    }

    @Override
    public void storeAll(Map<Long, Integer> map) {

    }

    @Override
    public void delete(Long key) {

    }

    @Override
    public void deleteAll(Collection<Long> keys) {

    }

    @Override
    public Integer load(Long key) {
        return null;
    }

    @Override
    public Map<Long, Integer> loadAll(Collection<Long> keys) {
        return null;
    }

    @Override
    public Set<Long> loadAllKeys() {
        return null;
    }
}
