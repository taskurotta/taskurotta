package ru.hazelcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * User: stukushin
 * Date: 30.05.13
 * Time: 12:18
 */
public class TestStorePolicy {

    private static final Logger logger = LoggerFactory.getLogger(TestStorePolicy.class);

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        int countNode = 1;

        for (int i = 0; i < countNode; i++) {
            logger.info("Start [{}] node", i);
            new Thread(new NodeStorePolicy(i * NodeStorePolicy.mapSize), "Node-" + i).start();
        }
    }
}
