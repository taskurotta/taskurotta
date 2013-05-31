package ru.hazelcast.store;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapLoaderLifecycleSupport;
import com.hazelcast.core.MapStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * User: stukushin
 * Date: 30.05.13
 * Time: 15:52
 */
public class MemoryMapStore implements MapStore, MapLoaderLifecycleSupport {

    private static final Logger logger = LoggerFactory.getLogger(MemoryMapStore.class);

    private Map store;

    @Override
    public synchronized void store(Object o, Object o2) {
        logger.debug("Store pair [{}]:[{}]", o, o2);
        store.put(o, o2);
    }

    @Override
    public synchronized void storeAll(Map map) {
        logger.debug("Store all [{}]", map);
        store.putAll(map);
    }

    @Override
    public synchronized void delete(Object o) {
        logger.debug("Delete for key [{}]", o);
        store.remove(o);
    }

    @Override
    public synchronized void deleteAll(Collection collection) {
        logger.debug("Delete all for keys [{}]", collection);
        for (Object key : collection) {
            store.remove(key);
        }
    }

    @Override
    public synchronized Object load(Object o) {
        logger.debug("Load for key [{}]", o);
        return store.get(o);
    }

    @Override
    public Map loadAll(Collection collection) {
        logger.debug("Load all for keys [{}]", collection);

        Map result = new HashMap();

        for (Object key : collection) {
            result.put(key, store.get(key));
            store.remove(key);
        }

        return result;
    }

    @Override
    public Set loadAllKeys() {
        logger.debug("Load all keys");
        return store.keySet();
    }

    @Override
    public synchronized void init(HazelcastInstance hazelcastInstance, Properties properties, String s) {
        logger.info("init with HazelcastInstance [{}], Properties [{}], Map name [{}]", hazelcastInstance, properties, s);
        if (store == null) {
            logger.info("Create new instance for store");
            store = new HashMap();
        } else {
            logger.info("Now store is [{}]([{}])", store, store.size());
        }
    }

    @Override
    public synchronized void destroy() {
        logger.info("Destroy store [{}]([{}])", store, store == null ? "empty" : store.size());
        store = null;
    }
}
