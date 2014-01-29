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
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.hazelcast.store.MongoQueueStore;

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

    private int maxSize;
    private int backupCount;
    private int asyncBackupsCount;
    private Integer memoryLimit;
    private Boolean binary;
    private Integer bulkLoad;

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


        if (logger.isDebugEnabled()) {//Logging debug monitor
            Thread monitor = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        while(true) {
                            StringBuilder sb = new StringBuilder("\nMongoMapStore statistics:\n");

                            sb.append(String.format("\ndelete count: %d mean: %8.3f oneMinuteRate: %8.3f",
                                    MongoMapStore.deleteTimer.count(), MongoMapStore.deleteTimer.mean(), MongoMapStore.deleteTimer.oneMinuteRate()));
                            sb.append(String.format("\nload count: %d mean: %8.3f oneMinuteRate: %8.3f",
                                    MongoMapStore.loadTimer.count(), MongoMapStore.loadTimer.mean(), MongoMapStore.loadTimer.oneMinuteRate()));
                            sb.append(String.format("\nstore count: %d mean: %8.3f oneMinuteRate: %8.3f",
                                    MongoMapStore.storeTimer.count(), MongoMapStore.storeTimer.mean(), MongoMapStore.storeTimer.oneMinuteRate()));

                            sb.append("\nMongo Queues statistics:");
                            sb.append(String.format("\ndelete count: %d mean: %8.3f oneMinuteRate: %8.3f",
                                    MongoQueueStore.deleteTimer.count(), MongoQueueStore.deleteTimer.mean(), MongoQueueStore.deleteTimer.oneMinuteRate()));
                            sb.append(String.format("\nload count: %d mean: %8.3f oneMinuteRate: %8.3f",
                                    MongoQueueStore.deleteTimer.count(), MongoQueueStore.loadTimer.mean(), MongoQueueStore.loadTimer.oneMinuteRate()));
                            sb.append(String.format("\nstore count: %d mean: %8.3f oneMinuteRate: %8.3f",
                                    MongoQueueStore.deleteTimer.count(), MongoQueueStore.storeTimer.mean(), MongoQueueStore.storeTimer.oneMinuteRate()));

                            logger.debug(sb.toString());

                            Thread.sleep(5*60*1000l);
                        }
                    } catch (Throwable e) {
                        logger.debug("Stopping debug monitor due to error", e);
                    }

                }
            });
            monitor.setName("MongoMapStore-debug-monitor#"+getClass().getName());
            monitor.setDaemon(true);
            monitor.start();
        }

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
            logger.debug("For queue name [{}] add config [{}]", queueName, qc);

            hzInstance.getQueue(queueName);//ensures HZ queue initialization
        } finally {
            queueConfigLock.unlock();
        }
    }

    public QueueStoreConfig createQueueStoreConfig(String queueName) {
        QueueStoreConfig queueStoreConfig = new QueueStoreConfig();

        queueStoreConfig.setProperty("binary", binary.toString());
        queueStoreConfig.setProperty("memory-limit", memoryLimit.toString());
        queueStoreConfig.setProperty("bulk-load", bulkLoad.toString());
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
