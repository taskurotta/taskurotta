package ru.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * User: stukushin
 * Date: 30.05.13
 * Time: 14:56
 */
public class NodeOverflow implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NodeOverflow.class);

    private Config config;

    private String hzMapName = "hzMap";
    public static int mapSize = 10;
    private int startPosition;

    public NodeOverflow(int startPosition) {
        this.startPosition = startPosition;
    }

    @Override
    public void run() {
        Map<Integer, Integer> originalMap = new HashMap<>();

        config = Utils.getConfig();
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        logger.info("Before filling, hazelcast map [{}]([{}]) , original map [{}]([{}])", Utils.hzMapToString(instance.getMap(hzMapName)), instance.getMap(hzMapName).size(), originalMap, originalMap.size());

        fill(originalMap, instance.getMap(hzMapName), startPosition, mapSize + startPosition);
        logger.info("After first adding [{}], hazelcast map [{}]([{}]) , original map [{}]([{}])", mapSize, Utils.hzMapToString(instance.getMap(hzMapName)), instance.getMap(hzMapName).size(), originalMap, originalMap.size());

        fill(originalMap, instance.getMap(hzMapName), startPosition + mapSize, startPosition + mapSize * 2);
        logger.info("After second adding [{}], hazelcast map [{}]([{}]) , original map [{}]([{}])", mapSize, Utils.hzMapToString(instance.getMap(hzMapName)), instance.getMap(hzMapName).size(), originalMap, originalMap.size());

        logger.info("Original map and hazelcast map is equals = [{}]", originalMap.equals(instance.getMap(hzMapName)));
    }

    private void fill(Map originalMap, IMap hzMap, int start, int length) {
        logger.info("Add [{}] elements from [{}] to [{}]", length - start, start, length);
        for (int i = start; i < length; i++) {
            originalMap.put(i, i);
            hzMap.put(i, i);
        }
    }


}
