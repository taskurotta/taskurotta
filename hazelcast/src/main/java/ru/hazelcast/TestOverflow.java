package ru.hazelcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 30.05.13
 * Time: 12:18
 */
public class TestOverflow {

    private static final Logger logger = LoggerFactory.getLogger(TestOverflow.class);

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        int countNode = 2;

        for (int i = 0; i < countNode; i++) {
            logger.info("Start [{}] node", i);
            new Thread(new NodeOverflow(i * NodeOverflow.mapSize), "Node-" + i).start();

            TimeUnit.SECONDS.sleep(1);
        }
    }
}
