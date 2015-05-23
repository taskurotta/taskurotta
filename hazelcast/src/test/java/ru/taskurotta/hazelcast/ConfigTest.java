package ru.taskurotta.hazelcast;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.impl.MockCachedQueueStore;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 */
public class ConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(ConfigTest.class);

    private static String QUEUE_NAME = "testQueue";


    @Ignore
    @Test
    public void failTest() {

        // create mock store with 4 items
        MockCachedQueueStore store = new MockCachedQueueStore();
        store.store(1L, 1L);
        store.store(2L, 2L);
        store.store(3L, 3L);
        store.store(4L, 4L);

        // try to poll all tree objects from Hazelcast CachedQueue

        // actor poll task(1) from hz1

        Config config1 = new Config();
        CachedQueueServiceConfig cachedQueueServiceConfig1 = CachedQueueServiceConfig.registerServiceConfig(config1);
        addQueueConfig(config1, store);

        HazelcastInstance hz1 = Hazelcast.newHazelcastInstance(config1);

        // configure queue on demand

        CachedQueue cachedQueue1 = CachedQueueServiceConfig.getCachedQueue(hz1, QUEUE_NAME);
        assertNotNull(cachedQueue1.poll());

        // actor poll task(2) from hz2

        Config config2 = new Config();
        CachedQueueServiceConfig cachedQueueServiceConfig2 = CachedQueueServiceConfig.registerServiceConfig(config2);
        addQueueConfig(config2, store);

        HazelcastInstance hz2 = Hazelcast.newHazelcastInstance(config2);

        // configure queue on demand

        CachedQueue cachedQueue2 = CachedQueueServiceConfig.getCachedQueue(hz2, QUEUE_NAME);
        assertNotNull(cachedQueue2.poll());

        // first server down
        hz1.shutdown();

        // actor poll task(3) from hz2
        assertNotNull(cachedQueue2.poll());

        // first server start again from scratch

        config1 = new Config();
        cachedQueueServiceConfig1 = CachedQueueServiceConfig.registerServiceConfig(config1);

        hz1 = Hazelcast.newHazelcastInstance(config1);

        // actor still poll task from hz2
        assertNotNull(cachedQueue2.poll());

    }

    private static void addQueueConfig(Config config, CachedQueueStore store) {
        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(config, QUEUE_NAME);
        {
            CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
            cachedQueueStoreConfig.setEnabled(true);
            cachedQueueStoreConfig.setBinary(false);
            cachedQueueStoreConfig.setBatchLoadSize(100);

            {
                cachedQueueStoreConfig.setStoreImplementation(store);
            }

            cachedQueueConfig.setQueueStoreConfig(cachedQueueStoreConfig);
        }
    }

    @Test
    public void loadHzConfigFromClasspathWithProperties() throws IOException {

        Properties properties = new Properties();
        properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("tsk.properties"));

        Config cfg = new ClasspathXmlConfig("tsk-hazelcast.xml", properties);

        System.err.println("Port: " + cfg.getNetworkConfig().getPort());
        System.err.println("Members: " + cfg.getNetworkConfig().getJoin().getTcpIpConfig().getMembers());

    }
}
