package ru.taskurotta.backend.hz.support;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.HazelcastInstance;
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

    public static final Logger logger = LoggerFactory.getLogger(HzQueueSpringConfigSupport.class);

    protected static final String BACKING_MAP_NAME_SUFFIX = "backingMap";

    private ApplicationContext applicationContext;
    private HazelcastInstance hzInstance;
    private String mapStoreBeanName;

    private int maxSizePerJvm = 0;
    private int evictionPercentage = 25;
    private int backupCount = 0;
    private int asyncBackupsCount = 0;
    private String evictionPolicy = "LRU";
    private int writeDelaySeconds = 0;

    public void createQueueConfig(String queueName) {

        String mapName = queueName + BACKING_MAP_NAME_SUFFIX;

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

        hzInstance.getConfig().addMapConfig(mc);
        logger.debug("Config for map name[{}] with mapstore bean [{}] added...", mapName, mapStoreBeanName);

        QueueConfig qc = new QueueConfig();
        qc.setName(queueName);
        qc.setBackingMapRef(mapName);
        qc.setMaxSizePerJVM(maxSizePerJvm);

        hzInstance.getConfig().addQueueConfig(qc);
        logger.debug("Config for queue name[{}] with mapstore bean [{}] added...", queueName, mapStoreBeanName);
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
}
