package ru.taskurotta.backend.hz.queue.delay;

import com.hazelcast.core.IMap;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:35 AM
 */
public class BaseStorage implements Storage {

    private IMap<UUID, BaseStorageItem> storage;

    public BaseStorage(IMap<UUID, BaseStorageItem> storage) {
        this.storage = storage;
    }

    @Override
    public boolean add(Object o, long delayTime, TimeUnit unit) {
        long enqueueTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delayTime, unit);
        BaseStorageItem storageItem = new BaseStorageItem(o, enqueueTime);

        while (storage.putIfAbsent(UUID.randomUUID(), storageItem) != null) {
            // Better safe than sorry! :)
        }

        return true;
    }

}
