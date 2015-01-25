package ru.taskurotta.hazelcast.queue;

import com.hazelcast.config.Config;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.QueueStoreConfig;
import com.hazelcast.config.ServiceConfig;
import com.hazelcast.config.ServicesConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.impl.QueueService;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Ignore
public class QueueTest {

    private static final Logger logger = LoggerFactory.getLogger(QueueTest.class);

    CachedQueue queue;

    @Before
    public void initCtx() {

        ServiceConfig queueServiceConfig = new ServiceConfig();
        queueServiceConfig.setEnabled(true);
        queueServiceConfig.setName(QueueService.SERVICE_NAME);
        queueServiceConfig.setClassName(QueueService.class.getName());


        QueueConfig qc = new QueueConfig();
        qc.setName("testQueue");
        qc.setMaxSize(Integer.MAX_VALUE);
        qc.setBackupCount(0);
        qc.setAsyncBackupCount(0);

        QueueStoreConfig queueStoreConfig = new QueueStoreConfig();

        queueStoreConfig.setProperty("binary", "false");
        queueStoreConfig.setProperty("memory-limit", "1000");
        queueStoreConfig.setProperty("bulk-load", "100");
        queueStoreConfig.setStoreImplementation(queueStoreFactory.newQueueStore(queueName, null));
        queueStoreConfig.setEnabled(true);

        qc.setQueueStoreConfig(createQueueStoreConfig(queueName));

        queueServiceConfig.setConfigObject(qc);



        Config cfg = new Config();
        ServicesConfig servicesConfig = cfg.getServicesConfig();
        servicesConfig.addServiceConfig(queueServiceConfig);

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
        queue = hazelcastInstance.getDistributedObject(QueueService.SERVICE_NAME, "testQueue");
    }

    @Test
    public void testDataLoss() {

        String item = "testItem";

        assertNull(queue.poll());

        queue.offer(item);

        String polledItem = (String) queue.poll();

        assertNotNull(polledItem);
        assertNull(queue.poll());

    }
}
