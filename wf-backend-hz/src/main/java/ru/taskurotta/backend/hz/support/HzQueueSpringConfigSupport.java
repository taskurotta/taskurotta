package ru.taskurotta.backend.hz.support;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Bean for creating configuration for queues with backing map stores at runtime
 * Uses named spring bean as mapStore implementation
 * User: dimadin
 * Date: 13.08.13 18:21
 */
public class HzQueueSpringConfigSupport implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueSpringConfigSupport.class);

    private static final String BACKING_MAP_NAME_SUFFIX = "backingMap";
    private static final String MAP_CONFIG_LOCK = "mapConfigLock";
    private static final String QUEUE_CONFIG_LOCK = "queueConfigLock";


    private ApplicationContext applicationContext;
    private HazelcastInstance hzInstance;
    private String mapStoreBeanName;

    private int maxSize = 100;
    private int maxSizePerJvm = 0;
    private int evictionPercentage = 25;
    private int backupCount = 0;
    private int asyncBackupsCount = 0;
    private String evictionPolicy = "LRU";
    private int writeDelaySeconds = 0;


    private ILock mapConfigLock;
    private ILock queueConfigLock;


    public HzQueueSpringConfigSupport(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
        this.mapConfigLock = hzInstance.getLock(MAP_CONFIG_LOCK);
        this.queueConfigLock = hzInstance.getLock(QUEUE_CONFIG_LOCK);
    }

    public void createQueueConfig(String queueName) {
        try {
            queueConfigLock.lock();

            if(isQueueExists(queueName)) {
                logger.debug("Skip creating queue[{}] config: it already exists...", queueName);
                return;
            }
            String mapName = queueName + BACKING_MAP_NAME_SUFFIX;
            createMapConfig(mapName);

            QueueConfig qc = new QueueConfig();
            qc.setName(queueName);
            qc.setBackingMapRef(mapName);
            qc.setMaxSizePerJVM(maxSizePerJvm);

            hzInstance.getConfig().addQueueConfig(qc);
            logger.debug("Config for queue name[{}] with mapstore bean [{}] added...", queueName, mapStoreBeanName);

        } finally {
            queueConfigLock.unlock();
        }
    }

    public void createMapConfig(String mapName) {
        try {
            mapConfigLock.lock();

            if(isMapExists(mapName)) {
                logger.debug("Skip creating map[{}] config: it already exists...", mapName);
                return;
            }

            MapStoreConfig msc = new MapStoreConfig();
            msc.setEnabled(true);
            msc.setImplementation(applicationContext.getBean(mapStoreBeanName));
            msc.setWriteDelaySeconds(writeDelaySeconds);

            MapConfig mc = new MapConfig();
            mc.setName(mapName);
            mc.setEvictionPercentage(evictionPercentage);
            mc.setBackupCount(backupCount);
            mc.setAsyncBackupCount(asyncBackupsCount);
            mc.setEvictionPolicy(evictionPolicy);
            mc.setMapStoreConfig(msc);

            MaxSizeConfig maxSizeConfig = new MaxSizeConfig();
            maxSizeConfig.setSize(maxSize);
            maxSizeConfig.setMaxSizePolicy(MaxSizeConfig.POLICY_USED_HEAP_SIZE);
            mc.setMaxSizeConfig(maxSizeConfig);

            hzInstance.getConfig().addMapConfig(mc);
            logger.debug("Config for map name[{}] with mapstore bean [{}] added...", mapName, mapStoreBeanName);

        } finally {
            mapConfigLock.unlock();
        }
    }

    private boolean isMapExists(String name) {
       boolean result = false;
       for(Instance inst: hzInstance.getInstances()) {
           if(inst.getInstanceType().isMap() && name.equals(((IMap) inst).getName())) {
               result = true;
               break;
           }
       }
       return result;
    }

    private boolean isQueueExists(String name) {
        boolean result = false;
        for(Instance inst: hzInstance.getInstances()) {
            if(inst.getInstanceType().isQueue() && name.equals(((IQueue) inst).getName())) {
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

    public void setMapStoreBeanName(String mapStoreBeanName) {
        this.mapStoreBeanName = mapStoreBeanName;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void setMaxSizePerJvm(int maxSizePerJvm) {
        this.maxSizePerJvm = maxSizePerJvm;
    }

    public void setEvictionPercentage(int evictionPercentage) {
        this.evictionPercentage = evictionPercentage;
    }

    public void setBackupCount(int backupCount) {
        this.backupCount = backupCount;
    }

    public void setAsyncBackupsCount(int asyncBackupsCount) {
        this.asyncBackupsCount = asyncBackupsCount;
    }

    public void setEvictionPolicy(String evictionPolicy) {
        this.evictionPolicy = evictionPolicy;
    }

    public void setWriteDelaySeconds(int writeDelaySeconds) {
        this.writeDelaySeconds = writeDelaySeconds;
    }
}
