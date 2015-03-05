package ru.taskurotta.hazelcast.queue.impl;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Partition;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueSizeConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 */
public class QueueMigrationTest {

    private static final Logger logger = LoggerFactory.getLogger(QueueMigrationTest.class);

    private static String QUEUE_NAME = "testQueue";

    @Ignore
    @Test
    public void queueMigrationTest() throws InterruptedException {

        Config cfg = new Config();
        CachedQueueServiceConfig cachedQueueServiceConfig = CachedQueueServiceConfig.registerServiceConfig(cfg);

        // temporary start two nodes of cluster
        HazelcastInstance hazelcastInstance1 = Hazelcast.newHazelcastInstance(cfg);
        HazelcastInstance hazelcastInstance2 = Hazelcast.newHazelcastInstance(cfg);

        String hz1PartitionKey = null;

        // find key for second hazelcast instance
        for (int i = 0; i < 1000; i++) {
            String key = "" + i;

            Partition partition = hazelcastInstance2.getPartitionService().getPartition(key);
            if (hazelcastInstance2.getCluster().getLocalMember().equals(partition.getOwner())) {
                hz1PartitionKey = key;

                // shutdown second node
                hazelcastInstance2.shutdown();
                TimeUnit.MILLISECONDS.sleep(1000l);
                break;
            }
        }

        if (hz1PartitionKey == null) {
            logger.debug("Can not find partition key");
            return;
        }

        // configure queue with partition key suffix
        String queueFullName = QUEUE_NAME + "@" + hz1PartitionKey;

        cachedQueueServiceConfig.getSizeConfig().setMaxSizePolicy(CachedQueueSizeConfig.SizePolicy.PER_NODE);

        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(cfg, QUEUE_NAME);
        cachedQueueConfig.setCacheSize(5);

        {
            CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
            cachedQueueStoreConfig.setEnabled(true);
            cachedQueueStoreConfig.setBinary(false);
            cachedQueueStoreConfig.setBatchLoadSize(100);

            {
                MockCachedQueueStore store = new MockCachedQueueStore();
                cachedQueueStoreConfig.setStoreImplementation(store);
            }

            cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);
        }


        // enqueue items to single node
        CachedQueue queue1 = hazelcastInstance1.getDistributedObject(CachedQueue.class.getName(), queueFullName);

        for (int i = 0; i < 7; i++) {
            queue1.offer(i);
        }

        // start second node. Queue should migrate to it => Queue should be removed on first node and instantiated
        // on second node.
        hazelcastInstance2 = Hazelcast.newHazelcastInstance(cfg);

        CachedQueue queue2 = hazelcastInstance2.getDistributedObject(CachedQueue.class.getName(), queueFullName);

        // poll same items from second node
        for (int i = 0; i < 5; i++) {
            assertNotNull(queue2.poll());
        }

        // shutdown second node
        // Queue should migrate to first node => Queue should be removed on second node and instantiated
        // on first node.
        hazelcastInstance2.shutdown();
        TimeUnit.MILLISECONDS.sleep(1000l);

        // fully drain queue
        for (int i = 0; i < 2; i++) {
            assertNotNull(queue1.poll());
        }

        // queue should be empty
        assertNull(queue1.poll());

    }

}
