package ru.taskurotta.hazelcast.queue.store.mongodb;

import com.mongodb.DB;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStoreFactory;
import ru.taskurotta.mongodb.driver.BSerializationService;

public class MongoCachedQueueStorageFactory implements CachedQueueStoreFactory {

    private DB mongoDB;
    private BSerializationService serializationService;

    public MongoCachedQueueStorageFactory(DB mongoDB, BSerializationService serializationService) {
        this.mongoDB = mongoDB;
        this.serializationService = serializationService;
    }

    public CachedQueueStore newQueueStore(String name, CachedQueueStoreConfig config) {
        return new MongoCachedQueueStore(name, mongoDB, config, serializationService);
    }

}
