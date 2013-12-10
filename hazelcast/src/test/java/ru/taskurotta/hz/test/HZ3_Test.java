package ru.taskurotta.hz.test;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import static junit.framework.Assert.assertEquals;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.QueueStoreConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.MapStore;
import com.hazelcast.core.QueueStore;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.backend.queue.TaskQueueItem;

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

            HazelcastInstance hz = Hazelcast.newHazelcastInstance(cfg);

            IQueue<TaskQueueItem> queue = hz.getQueue("TestQueue");


            System.out.println("Putttt.....");

            int size = 200000;
            for (int i = 0; i < size; i++) {
                TaskQueueItem item = new TaskQueueItem();
                item.setTaskId(UUID.randomUUID());
                item.setProcessId(UUID.randomUUID());
                item.setCreatedDate(new Date());
                item.setStartTime(new Date().getTime());
                item.setEnqueueTime(new Date().getTime());
                item.setQueueName(queue.getName());
                item.setTaskList("123213123123132212313");
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
                TaskQueueItem data = queue.poll();

                data.setStartTime(new Date().getTime());
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
