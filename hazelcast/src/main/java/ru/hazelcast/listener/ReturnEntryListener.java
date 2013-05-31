package ru.hazelcast.listener;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: stukushin
 * Date: 31.05.13
 * Time: 13:19
 */
public class ReturnEntryListener implements EntryListener<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ReturnEntryListener.class);

    private String mapName;
    private HazelcastInstance hazelcastInstance;

    public ReturnEntryListener(HazelcastInstance hazelcastInstance, String mapName) {
        this.hazelcastInstance = hazelcastInstance;
        this.mapName = mapName;
    }

    @Override
    public void entryAdded(EntryEvent entryEvent) {
        logger.debug("Add entry [{}]:[{}]. Event [{}]", entryEvent.getKey(), entryEvent.getValue(), entryEvent);
    }

    @Override
    public void entryRemoved(EntryEvent entryEvent) {
        logger.debug("Remove entry [{}]:[{}]. Event [{}]", entryEvent.getKey(), entryEvent.getValue(), entryEvent);
        hazelcastInstance.getMap(mapName).put(entryEvent.getKey(), entryEvent.getValue());
        logger.debug("Return entry [{}]:[{}] to map [{}]", entryEvent.getKey(), entryEvent.getValue(), mapName);
    }

    @Override
    public void entryUpdated(EntryEvent entryEvent) {
        logger.debug("Update entry [{}]:[{}]. Event [{}]", entryEvent.getKey(), entryEvent.getValue(), entryEvent);
    }

    @Override
    public void entryEvicted(EntryEvent entryEvent) {
        logger.debug("Evict entry [{}]:[{}]. Event [{}]", entryEvent.getKey(), entryEvent.getValue(), entryEvent);
        hazelcastInstance.getMap(mapName).put(entryEvent.getKey(), entryEvent.getValue());
        logger.debug("Return entry [{}]:[{}] to map [{}]", entryEvent.getKey(), entryEvent.getValue(), mapName);
    }
}
