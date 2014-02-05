package ru.taskurotta.hz.test.support;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapLoaderLifecycleSupport;
import com.hazelcast.core.MapStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 04.02.14 18:53
 */
public class FailingMapStore implements MapStore, MapLoaderLifecycleSupport {

    private int failAfter;
    private MapStore mapStore;
    private MapLoaderLifecycleSupport mapLoaderLifecycleSupport;

    private static final Logger logger = LoggerFactory.getLogger(FailingMapStore.class);

    private AtomicInteger counter = new AtomicInteger(0);

    public FailingMapStore (MapStore mapStore, MapLoaderLifecycleSupport mapLoaderLifecycleSupport, int failAfter) {
        this.failAfter = failAfter;
        this.mapStore = mapStore;
        this.mapLoaderLifecycleSupport = mapLoaderLifecycleSupport;
    }

    @Override
    public void store(Object key, Object value) {
        logger.info("Storing object: key[{}], value [{}]", key, value);
        if (counter.incrementAndGet() > failAfter) {
            throw new RuntimeException("Mongo is unavailable");
        }
        mapStore.store(key, value);
    }

    @Override
    public void storeAll(Map map) {
        mapStore.storeAll(map);
    }

    @Override
    public void delete(Object o) {
        mapStore.delete(o);
    }

    @Override
    public void deleteAll(Collection collection) {
        mapStore.deleteAll(collection);
    }

    @Override
    public Object load(Object o) {
        return mapStore.load(o);
    }

    @Override
    public Map loadAll(Collection collection) {
        return mapStore.loadAll(collection);
    }

    @Override
    public Set loadAllKeys() {
        return mapStore.loadAllKeys();
    }

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties properties, String s) {
        mapLoaderLifecycleSupport.init(hazelcastInstance, properties, s);
    }

    @Override
    public void destroy() {
        mapLoaderLifecycleSupport.destroy();
    }
}
