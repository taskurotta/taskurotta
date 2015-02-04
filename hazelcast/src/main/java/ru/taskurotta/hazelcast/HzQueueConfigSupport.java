package ru.taskurotta.hazelcast;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStoreFactory;

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
    private CachedQueueStoreFactory cachedQueueStoreFactory;

    private int cacheSize;
    private Boolean binary;
    private Integer batchLoadSize;
    private String objectClassName;

    private ILock queueConfigLock;

    public HzQueueConfigSupport(HazelcastInstance hzInstance, CachedQueueStoreFactory cachedQueueStoreFactory,
                                int cacheSize, boolean binary, int batchLoadSize, String objectClassName) {
        this.hzInstance = hzInstance;
        this.cachedQueueStoreFactory = cachedQueueStoreFactory;
        this.cacheSize = cacheSize;
        this.binary = binary;
        this.batchLoadSize = batchLoadSize;
        this.objectClassName = objectClassName;

        this.queueConfigLock = hzInstance.getLock(QUEUE_CONFIG_LOCK);

    }

    public boolean createQueueConfig(String queueName) {
        try {
            queueConfigLock.lock();

            if (isQueueExists(queueName)) {
                logger.debug("Skip creating queue[{}] config: it already exists...", queueName);
                return false;
            }

            CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(hzInstance.getConfig(),
                    queueName);
            cachedQueueConfig.setCacheSize(cacheSize);

            cachedQueueConfig.setQueueStoreConfig(createQueueStoreConfig(queueName));

            logger.debug("For queue name [{}] add config [{}]", queueName, cachedQueueConfig);

            //ensures HZ queue initialization
            hzInstance.getDistributedObject(CachedQueue.class.getName(), queueName);

            return true;
        } finally {
            queueConfigLock.unlock();
        }
    }

    public CachedQueueStoreConfig createQueueStoreConfig(String queueName) {

        CachedQueueStoreConfig cachedQueueStoreConfig = new CachedQueueStoreConfig();
        cachedQueueStoreConfig.setEnabled(true);
        cachedQueueStoreConfig.setBinary(binary);
        cachedQueueStoreConfig.setBatchLoadSize(batchLoadSize);
        cachedQueueStoreConfig.setObjectClassName(objectClassName);

        cachedQueueStoreConfig.setStoreImplementation(cachedQueueStoreFactory.newQueueStore(queueName, cachedQueueStoreConfig));

        return cachedQueueStoreConfig;
    }

    private boolean isQueueExists(String name) {
        boolean result = false;
        for (DistributedObject inst : hzInstance.getDistributedObjects()) {
            if ((inst instanceof CachedQueue) && name.equals(inst.getName())) {
                result = true;
                break;
            }
        }
        return result;
    }
}
