package ru.taskurotta.backend.hz.support;

import java.util.Properties;

import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.QueueStoreConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Bean for creating configuration for queues with backing map stores at runtime
 * Uses named spring bean as mapStore implementation
 * User: dimadin
 * Date: 13.08.13 18:21
 */
public class HzQueueSpringConfigSupport implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueSpringConfigSupport.class);

    private static final String BACKING_MAP_NAME_SUFFIX = "backingMap";
    private static final String QUEUE_CONFIG_LOCK = "queueConfigLock";


    private ApplicationContext applicationContext;
    private HazelcastInstance hzInstance;


    //    private String queueStoreBeanName;
    private int maxSize = 100;
    private int backupCount = 0;
    private int asyncBackupsCount = 0;

    private int memoryLimit = 100;
    private boolean binary = false;
    private int bulkLoad = 10;


    private ILock queueConfigLock;


    public HzQueueSpringConfigSupport(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
        this.queueConfigLock = hzInstance.getLock(QUEUE_CONFIG_LOCK);
    }

    public void createQueueConfig(String queueName) {
        try {
            queueConfigLock.lock();

            if (isQueueExists(queueName)) {
                logger.debug("Skip creating queue[{}] config: it already exists...", queueName);
                return;
            }
            String mapName = queueName + BACKING_MAP_NAME_SUFFIX;

            QueueConfig qc = new QueueConfig();
            qc.setName(queueName);
            qc.setMaxSize(maxSize);

            qc.setQueueStoreConfig(createQueueStoreConfig(queueName));

            hzInstance.getConfig().addQueueConfig(qc);
            logger.debug("Config for queue name[{}] added...", queueName);

        } finally {
            queueConfigLock.unlock();
        }
    }

    public QueueStoreConfig createQueueStoreConfig(String queueName) {
        QueueStoreConfig result = new QueueStoreConfig(new MongoQueueStore(queueName + ".backingMap", (MongoTemplate) applicationContext.getBean("mongoTemplate")));
        result.setEnabled(true);
        Properties properties = new Properties();
        properties.put("binary", this.binary);
        properties.put("memory-limit", this.memoryLimit);
        properties.put("bulk-load", this.bulkLoad);
        result.setProperties(properties);
        return result;
    }

    private boolean isMapExists(String name) {
        boolean result = false;
        for (DistributedObject inst : hzInstance.getDistributedObjects()) {
            if ((inst instanceof IMap) && name.equals(inst.getName())) {
                result = true;
                break;
            }
        }
        return result;
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }


    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }


    public void setBackupCount(int backupCount) {
        this.backupCount = backupCount;
    }

    public void setAsyncBackupsCount(int asyncBackupsCount) {
        this.asyncBackupsCount = asyncBackupsCount;
    }

    public boolean isBinary() {
        return binary;
    }

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    public int getBackupCount() {
        return backupCount;
    }

    public int getAsyncBackupsCount() {
        return asyncBackupsCount;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(int memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public int getBulkLoad() {
        return bulkLoad;
    }

    public void setBulkLoad(int bulkLoad) {
        this.bulkLoad = bulkLoad;
    }
}
