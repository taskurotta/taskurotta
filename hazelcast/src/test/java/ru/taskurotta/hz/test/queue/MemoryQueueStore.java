package ru.taskurotta.hz.test.queue;

import com.hazelcast.core.QueueStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: stukushin
 * Date: 16.01.14
 * Time: 18:42
 */
public class MemoryQueueStore implements QueueStore<Integer> {

    private ConcurrentHashMap<Long, Integer> store = new ConcurrentHashMap<>();

    @Override
    public void store(Long key, Integer value) {
        store.put(key, value);
    }

    @Override
    public void storeAll(Map<Long, Integer> map) {
        store.putAll(map);
    }

    @Override
    public void delete(Long key) {
        store.remove(key);
    }

    @Override
    public void deleteAll(Collection<Long> keys) {
        for (Long key: keys) {
            store.remove(key);
        }
    }

    @Override
    public Integer load(Long key) {
        return store.get(key);
    }

    @Override
    public Map<Long, Integer> loadAll(Collection<Long> keys) {
        Map<Long, Integer> result = new HashMap<>(keys.size());

        for (Long key: keys) {
            result.put(key, store.get(key));
        }

        return result;
    }

    @Override
    public Set<Long> loadAllKeys() {
        return store.keySet();
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}
