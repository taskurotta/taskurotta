package ru.taskurotta.hz.test;

import com.hazelcast.config.Config;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.QueueStoreConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.mongodb.DBAddress;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.store.MongoQueueStore;

import java.net.UnknownHostException;

/**
 * Created by void 17.01.14 11:20
 */
@Ignore
public class QueueTest {
    protected final static Logger log = LoggerFactory.getLogger(QueueTest.class);

    private static final int MAX_ITEMS = 100000;
    private static final String QUEUE_NAME = "testQueue";

    private MongoTemplate getMongoTemplate() throws UnknownHostException {
        return new MongoTemplate(new MongoClient("127.0.0.1"), "test");
    }

    private Config getServerConfig() throws Exception {
        Config config = new Config();
        //config.setProperty("hazelcast.initial.min.cluster.size","2");

        QueueConfig queueConfig = new QueueConfig();
        queueConfig.setBackupCount(0);
        queueConfig.setName(QUEUE_NAME);
        config.addQueueConfig(queueConfig);

        QueueStoreConfig queueStoreConfig = new QueueStoreConfig();
        queueStoreConfig.setStoreImplementation(new MongoQueueStore(QUEUE_NAME, getMongoTemplate()));
        queueStoreConfig.setProperty("memory-limit", "1000");
        queueConfig.setQueueStoreConfig(queueStoreConfig);

        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        tcpIpConfig.addMember("127.0.0.1");

        return config;
    }

    @Test
    public void start() throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(getServerConfig());
        log.info("server instance started");
        Thread.sleep(300000);
    }

    @Test
    public void populateQueue() throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(getServerConfig());

        IQueue<Object> testQueue = hazelcastInstance.getQueue(QUEUE_NAME);
        log.info("Queue size before: {}", testQueue.size());

        for (int i=0; i<MAX_ITEMS; i++) {
            testQueue.add((long) i);
        }

        log.info("Queue size after: {}", testQueue.size());
    }

    @Test
    public void queuePollTest() throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(getServerConfig());

        IQueue<Object> testQueue = hazelcastInstance.getQueue(QUEUE_NAME);
        log.info("Queue size: {}", testQueue.size());
        long start = System.currentTimeMillis();

        int count = 0;
        while (null != testQueue.poll()) {
            count ++;
        }

        long time = System.currentTimeMillis() - start;
        log.info("can get {} items from queue; {} pps", count, (double)count / (double)time);
    }
}
