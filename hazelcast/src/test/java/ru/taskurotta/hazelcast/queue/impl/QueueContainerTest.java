package ru.taskurotta.hazelcast.queue.impl;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.impl.NodeEngineImpl;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.impl.proxy.QueueProxyImpl;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class QueueContainerTest {

    private static final Logger logger = LoggerFactory.getLogger(QueueContainerTest.class);

    private static String QUEUE_NAME = "testQueue";

    CachedQueue queue;
    QueueContainer container;
    MockCachedQueueStore store;

    @Before
    public void initCtx() throws Exception {

        Config cfg = new Config();

        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(cfg, QUEUE_NAME);
        cachedQueueConfig.setCacheSize(5);

        {
            CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
            cachedQueueStoreConfig.setEnabled(true);
            cachedQueueStoreConfig.setBinary(false);
            cachedQueueStoreConfig.setBatchLoadSize(100);

            {
                store = new MockCachedQueueStore();
                cachedQueueStoreConfig.setStoreImplementation(store);
            }

            cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);
        }

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
        queue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), QUEUE_NAME);

        QueueService queueService = (QueueService) ((NodeEngineImpl) ((QueueProxyImpl) queue).getNodeEngine())
                .getService(CachedQueue.class.getName());

        container = queueService.getOrCreateContainer(QUEUE_NAME, false);

        assertNotNull(queue);
        assertNotNull(container);
    }

    @Test
    public void test() {
        logger.debug("Start...");

        AtomicInteger counter = new AtomicInteger(0);

        container.resizeBuffer(6);

        assertContainerState(0, -1, false, 6, 0);

        logger.debug("Finish");
    }

    private void assertContainerState(long headId, long tailId, boolean bufferClosed, int maxBufferSize, int
            bufferSize) {

        assertEquals(container.headId, headId);
        assertEquals(container.tailId, tailId);
        assertEquals(container.bufferClosed, bufferClosed);
        assertEquals(container.maxBufferSize, maxBufferSize, maxBufferSize);
        assertEquals(container.buffer.size(), bufferSize);
    }

    private void addToQueue(CachedQueue queue, AtomicInteger counter, int quantity) {

        logger.debug("Add new {} items. counter is {}", quantity, counter.get());

        for (int i = 0; i < quantity; i++) {
            String item = "" + counter.getAndIncrement();
            queue.add(item);
        }
    }


    private void pollFromQueue(CachedQueue queue, AtomicInteger counter, int quantity) {
        for (int i = 0; i < quantity; i++) {

            String item = (String) queue.poll();
            Assert.assertNotNull(item);
        }
    }



}
