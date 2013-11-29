package ru.taskurotta.backend.hz.support;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Created with IntelliJ IDEA.
 * User: moroz
 * Date: 19.08.13
 * Time: 11:10
 * To change this template use File | Settings | File Templates.
 */
public class HzMapConfigSpringSupport implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(HzMapConfigSpringSupport.class);

    private ApplicationContext applicationContext;
    private HazelcastInstance hzInstance;
    private ILock mapConfigLock;
    private static final String MAP_CONFIG_LOCK = "mapConfigLock";
    private String mapStoreBeanName;
    private int writeDelaySeconds;
    private int evictionPercentage;
    private int backupCount;
    private int timeToLive;
    private int asyncBackupsCount;
    private String evictionPolicy;
    private int maxSize;

    public HzMapConfigSpringSupport(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
        this.mapConfigLock = hzInstance.getLock(MAP_CONFIG_LOCK);
    }

    public void createMapConfig (String mapName) {
        try {
            mapConfigLock.lock();

            if (isMapExists(mapName)) {
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
            mc.setTimeToLiveSeconds(timeToLive);
            mc.setEvictionPolicy(MapConfig.EvictionPolicy.valueOf(evictionPolicy));
            mc.setMapStoreConfig(msc);

            MaxSizeConfig maxSizeConfig = new MaxSizeConfig();
            maxSizeConfig.setSize(maxSize);
            maxSizeConfig.setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.USED_HEAP_SIZE);
            mc.setMaxSizeConfig(maxSizeConfig);

            hzInstance.getConfig().addMapConfig(mc);
            logger.debug("Config for map name[{}] with mapstore bean [{}] added...", mapName, mapStoreBeanName);

        } finally {
            mapConfigLock.unlock();
        }
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public HazelcastInstance getHzInstance() {
        return hzInstance;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    public ILock getMapConfigLock() {
        return mapConfigLock;
    }

    public void setMapConfigLock(ILock mapConfigLock) {
        this.mapConfigLock = mapConfigLock;
    }

    public String getMapStoreBeanName() {
        return mapStoreBeanName;
    }

    public void setMapStoreBeanName(String mapStoreBeanName) {
        this.mapStoreBeanName = mapStoreBeanName;
    }

    public int getAsyncBackupsCount() {
        return asyncBackupsCount;
    }

    public void setAsyncBackupsCount(int asyncBackupsCount) {
        this.asyncBackupsCount = asyncBackupsCount;
    }

    public int getBackupCount() {
        return backupCount;
    }

    public void setBackupCount(int backupCount) {
        this.backupCount = backupCount;
    }

    public int getEvictionPercentage() {
        return evictionPercentage;
    }

    public void setEvictionPercentage(int evictionPercentage) {
        this.evictionPercentage = evictionPercentage;
    }

    public String getEvictionPolicy() {
        return evictionPolicy;
    }

    public void setEvictionPolicy(String evictionPolicy) {
        this.evictionPolicy = evictionPolicy;
    }

    public static Logger getLogger() {
        return logger;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getWriteDelaySeconds() {
        return writeDelaySeconds;
    }

    public void setWriteDelaySeconds(int writeDelaySeconds) {
        this.writeDelaySeconds = writeDelaySeconds;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }
}
