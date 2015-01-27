package ru.taskurotta.hz.test;

import com.hazelcast.config.*;
import com.hazelcast.core.*;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.util.ConfigUtil;

import java.util.*;
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

        HazelcastInstance hz = Hazelcast.newHazelcastInstance(ConfigUtil.disableMulticast(cfg));
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
            mapConfig.setEvictionPolicy(EvictionPolicy.LFU);

            MaxSizeConfig maxSizeConfig = new MaxSizeConfig();
            maxSizeConfig.setSize(100);
            maxSizeConfig.setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.PER_NODE);

            mapConfig.setMaxSizeConfig(maxSizeConfig);


            Config cfg = new Config();
            cfg.addMapConfig(mapConfig);

            HazelcastInstance hz = Hazelcast.newHazelcastInstance(ConfigUtil.disableMulticast(cfg));

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

    @Ignore
    @Test
    public void queueTest() {

        int counter = 0;

        try {

            QueueConfig qc = new QueueConfig();
            qc.setName("TestQueue");

            qc.setQueueStoreConfig(createQueueStoreConfig(qc.getName()));

            Config cfg = new Config();
            cfg.addQueueConfig(qc);
            SerializationConfig serCfg = new SerializationConfig();

            HazelcastInstance hz = Hazelcast.newHazelcastInstance(ConfigUtil.disableMulticast(cfg));

            CachedQueue<TestQueueItem> queue = hz.getDistributedObject(CachedQueue.class.getName(), "TestQueue");


            System.out.println("Putttt.....");

            int size = 200000;
            for (int i = 0; i < size; i++) {
                TestQueueItem item = new TestQueueItem();
                item.setTaskId(UUID.randomUUID().toString());
                item.setProcessId(UUID.randomUUID().toString());
                queue.add(item);
                counter++;
                if (i % 10000 == 0) {
                    System.out.println("Put task № " + i);
                    System.out.println(i + ",\t free mem = " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb");
                }

            }

            System.out.println("Putted " + counter + " tasks");
            System.out.println("Getttt.....");

            for (int i = 0; i < size; i++) {
                TestQueueItem data = queue.poll();

                data.setStartDate(new Date().getTime());
                if (data != null) {
                    counter--;
                }

                if (i % 10000 == 0) {
                    System.out.println(i + ",\t free mem = " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb");
                }
            }

            System.out.println("Result " + counter);

        } finally {

            Hazelcast.shutdownAll();

        }

    }

    public QueueStoreConfig createQueueStoreConfig(String queueName) {

        QueueStoreConfig result = new QueueStoreConfig();
        result.setStoreImplementation(new QueueStore() {

            Map<Long, Object> map = new HashMap<>();

            @Override
            public void store(Long aLong, Object o) {
                map.put(aLong, o);
                System.out.println("Evicted " + map.size() + " tasks");
            }

            @Override
            public void storeAll(Map map) {
                map.putAll(map);
            }

            @Override
            public void delete(Long aLong) {
                map.remove(aLong);
            }

            @Override
            public void deleteAll(Collection collection) {
                for (Long aLong : (Collection<Long>) collection) {
                    map.remove(aLong);
                }
            }

            @Override
            public Object load(Long aLong) {
                return map.get(aLong);
            }

            @Override
            public Map loadAll(Collection longs) {
                Map<Long, Object> map1 = new HashMap<Long, Object>();
                for (Long aLong : (Collection<Long>) longs) {
                    map1.put(aLong, map.get(aLong));
                }
                return map1;
            }

            @Override
            public Set<Long> loadAllKeys() {
                return map.keySet();
            }


        });

        result.setEnabled(true);
        Properties properties = new Properties();
        properties.put("binary", true);
        properties.put("memory-limit", "1000");
        properties.put("bulk-load", "500");
        result.setProperties(properties);
        return result;
    }


}
