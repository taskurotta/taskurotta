package ru.taskurotta.hazelcast.queue.impl;

import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 */
public class MockCachedQueueStore implements CachedQueueStore {

    protected NavigableMap<Long, Object> storeMap = new ConcurrentSkipListMap<>();

    protected int lastLoadedCount = 0;

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

        lastLoadedCount = 0;

        Map resultMap = new HashMap();

        for (Long key : storeMap.keySet()) {

            if (key >= from && key <= to) {
                Object value = storeMap.get(key);
                if (value != null) {
                    lastLoadedCount++;
                    resultMap.put(key, value);
                }
            }
        }

        return resultMap;
    }

    @Override
    public Set<Long> loadAllKeys() {
        return storeMap.keySet();
    }

    @Override
    public long getMinItemId() {
        if (storeMap.size() == 0) return 0;
        return storeMap.firstKey();
    }

    @Override
    public long getMaxItemId() {
        if (storeMap.size() == 0) return -1;
        return storeMap.lastKey();
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
