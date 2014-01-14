package ru.taskurotta.hazelcast;

import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.QueueStoreConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.QueueStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Bean for creating configuration for queues with backing map stores at runtime
 * Uses named spring bean as mapStore implementation
 * User: dimadin
 * Date: 13.08.13 18:21
 */
public class HzQueueConfigSupport {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueConfigSupport.class);

    private static final String QUEUE_CONFIG_LOCK = "queueConfigLock";

    private HazelcastInstance hzInstance;
    private QueueStoreFactory queueStoreFactory;

    private int maxSize = 0;
    private int backupCount = 0;
    private int asyncBackupsCount = 0;
    private Integer memoryLimit = 100;
    private Boolean binary = false;
    private Integer bulkLoad = 10;

    private ILock queueConfigLock;

    public HzQueueConfigSupport(HazelcastInstance hzInstance, QueueStoreFactory queueStoreFactory,
                                int maxSize, int backupCount, int asyncBackupsCount,
                                int memoryLimit, boolean binary, int bulkLoad) {
        this.hzInstance = hzInstance;
        this.queueStoreFactory = queueStoreFactory;
        this.maxSize = maxSize;
        this.backupCount = backupCount;
        this.asyncBackupsCount = asyncBackupsCount;
        this.memoryLimit = memoryLimit;
        this.binary = binary;
        this.bulkLoad = bulkLoad;

        this.queueConfigLock = hzInstance.getLock(QUEUE_CONFIG_LOCK);
    }

    public void createQueueConfig(String queueName) {
        try {
            queueConfigLock.lock();

            if (isQueueExists(queueName)) {
                logger.debug("Skip creating queue[{}] config: it already exists...", queueName);
                return;
            }

            QueueConfig qc = new QueueConfig();
            qc.setName(queueName);
            qc.setMaxSize(maxSize);
            qc.setBackupCount(backupCount);
            qc.setAsyncBackupCount(asyncBackupsCount);

            qc.setQueueStoreConfig(createQueueStoreConfig(queueName));

            hzInstance.getConfig().addQueueConfig(qc);
            logger.debug("Config for queue name[{}] added...", queueName);

        } finally {
            queueConfigLock.unlock();
        }
    }

    public QueueStoreConfig createQueueStoreConfig(String queueName) {
        QueueStoreConfig queueStoreConfig = new QueueStoreConfig();

        Properties properties = new Properties();
        properties.setProperty("binary", binary.toString());
        properties.setProperty("memory-limit", memoryLimit.toString());
        properties.setProperty("bulk-load", bulkLoad.toString());
        queueStoreConfig.setProperties(properties);

        queueStoreConfig.setStoreImplementation(queueStoreFactory.newQueueStore(queueName, null));
        queueStoreConfig.setEnabled(true);

        return queueStoreConfig;
    }

    private boolean isQueueExists(String name) {
        boolean result = false;
        for (DistributedObject inst : hzInstance.getDistributedObjects()) {
            if ((inst instanceof IQueue) && name.equals(inst.getName())) {
                result = true;
                break;
            }
        }
        return result;
    }
}
