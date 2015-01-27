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

        AtomicInteger offerCounter = new AtomicInteger(0);
        AtomicInteger pollCounter = new AtomicInteger(0);

        // check initial state
        assertContainerState(0, -1, false, 5, 0, 0);

        // resize empty queue
        container.resizeBuffer(6);
        assertContainerState(0, -1, false, 6, 0, 0);

        // resize with same value
        container.resizeBuffer(6);
        assertContainerState(0, -1, false, 6, 0, 0);

        // add new items less then cache size
        addToQueue(offerCounter, 3);
        assertContainerState(0, 2, false, 6, 3, 3);

        // resize. cache size still longer then queue size
        container.resizeBuffer(5);
        assertContainerState(0, 2, false, 5, 3, 3);

        // buffer is opened
        // resize. cache size should drained to queue size
        container.resizeBuffer(3);
        assertContainerState(0, 2, true, 3, 3, 3);

        // buffer is closed
        // resize. cache size should drained to less then queue size
        container.resizeBuffer(2);
        assertContainerState(0, 2, true, 2, 2, 3);

        // resize cache to queue size
        assertEquals(0, store.lastLoadedCount);
        container.resizeBuffer(3);
        assertContainerState(0, 2, true, 3, 3, 3);
        assertEquals(1, store.lastLoadedCount);

        // buffer is closed
        // resize cache to grater than queue size
        container.resizeBuffer(5);
        assertContainerState(0, 2, false, 5, 3, 3);

        // buffer is open
        // resize. cache size should drained to less then queue size
        container.resizeBuffer(1);
        assertContainerState(0, 2, true, 1, 1, 3);

        // buffer is closed
        // resize cache to less than queue size
        container.resizeBuffer(2);
        assertContainerState(0, 2, true, 2, 2, 3);
        assertEquals(1, store.lastLoadedCount);

        // buffer is closed
        // resize cache to grater than queue size
        container.resizeBuffer(5);
        assertContainerState(0, 2, false, 5, 3, 3);
        assertEquals(1, store.lastLoadedCount);

        // buffer is open
        // resize cache down but grater than queue size
        store.lastLoadedCount = 0;
        container.resizeBuffer(4);
        assertContainerState(0, 2, false, 4, 3, 3);
        assertEquals(0, store.lastLoadedCount);

        // add new items till cache size
        addToQueue(offerCounter, 1);
        assertContainerState(0, 3, true, 4, 4, 4);

        // add new items grater cache size
        addToQueue(offerCounter, 2);
        assertContainerState(0, 5, true, 4, 4, 6);

        // buffer is closed: poll and buffer still closed
        pollFromQueue(pollCounter, 4);
        assertContainerState(4, 5, true, 4, 0, 2);

        // buffer is closed: poll one and load and open buffer
        pollFromQueue(pollCounter, 1);
        assertContainerState(5, 5, false, 4, 1, 1);
        assertEquals(1, container.size());

        // buffer is open: drain to zero
        pollFromQueue(pollCounter, 1);
        assertContainerState(6, 5, false, 4, 0, 0);
        assertEquals(0, container.size());

        logger.debug("Finish");
    }

    private void assertContainerState(long headId, long tailId, boolean bufferClosed, int maxBufferSize, int
            bufferSize, int storeSize) {

        assertEquals(headId, container.headId);
        assertEquals(tailId, container.tailId);
        assertEquals(bufferClosed, container.bufferClosed);
        assertEquals(maxBufferSize, container.maxBufferSize);
        assertEquals(bufferSize, container.buffer.size());
        assertEquals(storeSize, store.storeMap.size());
    }

    private void addToQueue(AtomicInteger counter, int quantity) {

        logger.debug("Add new {} items. counter is {}", quantity, counter.get());

        for (int i = 0; i < quantity; i++) {
            String item = "" + counter.incrementAndGet();
            queue.add(item);
        }
    }


    private void pollFromQueue(AtomicInteger counter, int quantity) {

        for (int i = 0; i < quantity; i++) {
            String expectedItem = "" + counter.incrementAndGet();
            String item = (String) queue.poll();

            Assert.assertNotNull(item);
            Assert.assertEquals(expectedItem, item);
        }
    }



}
