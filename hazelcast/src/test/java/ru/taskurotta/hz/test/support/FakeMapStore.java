package ru.taskurotta.hz.test.support;

import com.hazelcast.core.MapStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Date: 05.02.14 12:01
 */
public class FakeMapStore implements MapStore {

    public static final Logger logger = LoggerFactory.getLogger(FakeMapStore.class);

    @Override
    public void store(Object o, Object o2) {
        logger.info("Store object: key[{}], value [{}]", o, o2);
    }

    @Override
    public void storeAll(Map map) {
        for (Object key: map.keySet()) {
            store(key, map.get(key));
        }
    }

    @Override
    public void delete(Object o) {
        logger.info("Delete object [{}]", o);
    }

    @Override
    public void deleteAll(Collection collection) {
        for (Object o: collection) {
            delete(o);
        }
    }

    @Override
    public Object load(Object o) {
        logger.info("load object key [{}]", o);
        return null;
    }

    @Override
    public Map loadAll(Collection collection) {
        return null;
    }

    @Override
    public Set loadAllKeys() {
        return null;
    }
}
