package ru.taskurotta.hazelcast.queue.store.mongodb;

import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;

public class MongoCachedQueueStorageFactory {

    private MongoTemplate mongoTemplate;

    public MongoCachedQueueStorageFactory(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public CachedQueueStore newQueueStore(String name, CachedQueueStoreConfig config) {
        return new MongoCachedQueueStore(name, mongoTemplate, config);
    }
}
