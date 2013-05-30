package ru.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 30.05.13
 * Time: 14:56
 */
public class NodeStorePolicy implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NodeStorePolicy.class);

    private static String hzMapName = "hzMap";
    public static int mapSize = 10;
    public static int timeout = 5;
    private int startPosition;

    private HazelcastInstance instance;

    public NodeStorePolicy(int startPosition) {
        this.startPosition = startPosition;
    }

    @Override
    public void run() {
        Map<Integer, Integer> originalMap = new HashMap<>();

        Config config = Utils.getConfig();
        logger.info("Config = [{}]", config);
        instance = Hazelcast.newHazelcastInstance(config);

        logger.info("Before filling, hazelcast map [{}]([{}]) , original map [{}]([{}])", Utils.hzMapToString(instance.getMap(hzMapName)), instance.getMap(hzMapName).size(), originalMap, originalMap.size());

        fill(originalMap, startPosition, mapSize + startPosition);
        logger.info("After first adding [{}], hazelcast map [{}]([{}]) , original map [{}]([{}])", mapSize, Utils.hzMapToString(instance.getMap(hzMapName)), instance.getMap(hzMapName).size(), originalMap, originalMap.size());

        long sleep = timeout * 2;
        logger.info("Sleep for [{}] seconds for apply store policy", sleep);
        sleep(sleep);

        logger.info("After sleep hazelcast map [{}]([{}]), original map [{}]([{}])", Utils.hzMapToString(instance.getMap(hzMapName)), instance.getMap(hzMapName).size(), originalMap, originalMap.size());
        logger.info("Original map and hazelcast map is equals = [{}]", originalMap.equals(instance.getMap(hzMapName)));
    }

    private void fill(Map originalMap, int start, int length) {
        logger.info("Add [{}] elements from [{}] to [{}]", length - start, start, length);
        for (int i = start; i < length; i++) {
            originalMap.put(i, i);
            instance.getMap(hzMapName).put(i, i);
            instance.getMap(hzMapName).get(i);
        }
    }

    private void sleep(long timeout) {
        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
