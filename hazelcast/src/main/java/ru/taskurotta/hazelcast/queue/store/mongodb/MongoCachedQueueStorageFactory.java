package ru.taskurotta.hazelcast.queue.store.mongodb;

import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStoreFactory;
import ru.taskurotta.mongodb.driver.BSerializationService;

public class MongoCachedQueueStorageFactory implements CachedQueueStoreFactory {

    private MongoTemplate mongoTemplate;
    private BSerializationService serializationService;

    public MongoCachedQueueStorageFactory(MongoTemplate mongoTemplate, BSerializationService serializationService) {
        this.mongoTemplate = mongoTemplate;
        this.serializationService = serializationService;
    }

    public CachedQueueStore newQueueStore(String name, CachedQueueStoreConfig config) {
        return new MongoCachedQueueStore(name, mongoTemplate, config, serializationService);
    }

}
