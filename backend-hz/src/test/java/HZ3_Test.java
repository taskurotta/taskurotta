import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapStore;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import static junit.framework.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: moroz
 * Date: 15.08.13
 * Time: 12:53
 * To change this template use File | Settings | File Templates.
 */
public class HZ3_Test {

    @Test
    public void test() {
        Config cfg = new Config();
        HazelcastInstance hz = Hazelcast.newHazelcastInstance(cfg);
        Map<String, Object> myLockedObject = hz.getMap("test");
        Lock lock = hz.getLock("test");
        lock.lock();
        try {
            myLockedObject.put("123", "2121321");
        } finally {
            lock.unlock();
        }
    }

    @Ignore
    @Test
    public void evictionTest() {

        try {

            MapConfig mapConfig = new MapConfig("test");

            MapStoreConfig mapStoreConfig = new MapStoreConfig();
            mapStoreConfig.setImplementation(new MapStore<Object, Object>() {
                @Override
                public void store(Object key, Object value) {
                    //System.out.println("\n\nYayaya! key: " + key + " value: " + value + "\n\n");
                }

                @Override
                public void storeAll(Map<Object, Object> map) {
                }

                @Override
                public void delete(Object key) {
                }

                @Override
                public void deleteAll(Collection<Object> keys) {
                }

                @Override
                public Object load(Object key) {
                    int[] data = new int[1000];
                    data[0] = ((Integer) key).intValue();
                    return data;
                }

                @Override
                public Map<Object, Object> loadAll(Collection<Object> keys) {
                    return null;
                }

                @Override
                public Set<Object> loadAllKeys() {
                    return null;
                }
            });

            mapStoreConfig.setEnabled(true);
            mapConfig.setTimeToLiveSeconds(1);

            mapConfig.setMapStoreConfig(mapStoreConfig);
            mapConfig.setEvictionPercentage(80);
            mapConfig.setEvictionPolicy(MapConfig.EvictionPolicy.LFU);
            //mapConfig.setStorageType(MapConfig.StorageType.HEAP);

            MaxSizeConfig maxSizeConfig = new MaxSizeConfig();
            maxSizeConfig.setSize(100);
            maxSizeConfig.setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.PER_NODE);

            mapConfig.setMaxSizeConfig(maxSizeConfig);


            Config cfg = new Config();
            cfg.addMapConfig(mapConfig);

            HazelcastInstance hz = Hazelcast.newHazelcastInstance(cfg);

            IMap<Integer, int[]> map = hz.getMap("test");


            System.out.println("Putttt.....");

            int size = 1000000;
            for (int i = 0; i < size; i++) {
                int[] data = new int[1000];
                data[0] = i;

                map.set(i, data);
                //map.evict(i);

                if (i % 10000 == 0) {
                    System.out.println(i + ",\t free mem = " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb");
                }
            }

            System.out.println("Getttt.....");

            for (int i = 0; i < size; i++) {
                int[] data = map.get(i);
                data[0] = i;

                assertEquals(i, data[0]);

                //map.evict(i);

                if (i % 10000 == 0) {
                    System.out.println(i + ",\t free mem = " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb");
                }
            }

        } finally {

            Hazelcast.shutdownAll();

        }

    }
}
