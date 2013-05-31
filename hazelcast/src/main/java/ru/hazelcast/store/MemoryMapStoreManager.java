package ru.hazelcast.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: stukushin
 * Date: 30.05.13
 * Time: 16:19
 */
public class MemoryMapStoreManager {

    private static final Logger logger = LoggerFactory.getLogger(MemoryMapStoreManager.class);

    private static MemoryMapStore memoryMapStore = null;

    public synchronized static MemoryMapStore getMemoryMapStore() {
        if (memoryMapStore == null) {
            logger.info("Create MemoryMapStore instance");
            memoryMapStore = new MemoryMapStore();
        }

        logger.info("Return MemoryMapStore instance");

        return memoryMapStore;
    }
}
