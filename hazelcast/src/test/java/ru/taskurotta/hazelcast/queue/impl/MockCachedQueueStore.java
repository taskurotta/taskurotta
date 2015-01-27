package ru.taskurotta.hazelcast.queue.impl;

import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class MockCachedQueueStore implements CachedQueueStore {

    protected Map<Long, Object> storeMap = new ConcurrentHashMap();

    @Override
    public void store(Long key, Object value) {
        storeMap.put(key, value);
    }

    @Override
    public void delete(Long key) {
        storeMap.remove(key);
    }

    @Override
    public Object load(Long key) {
        return storeMap.get(key);
    }

    @Override
    public Map loadAll(long from, long to) {

        Map resultMap = new HashMap();

        for (Long key: storeMap.keySet()) {
            Object value = storeMap.get(key);
            if (value != null) {
                resultMap.put(key, value);
            }
        }

        return resultMap;
    }

    @Override
    public Set<Long> loadAllKeys() {

        return storeMap.keySet();
    }

    @Override
    public Map loadAll(Collection keys) {

        Map resultMap = new HashMap();
        resultMap.putAll(storeMap);

        return resultMap;
    }

    @Override
    public void deleteAll(Collection keys) {
        storeMap.clear();
    }

    @Override
    public void storeAll(Map map) {
        storeMap.putAll(map);
    }
}
