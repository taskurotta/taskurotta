package ru.taskurotta.hazelcast.store;

import com.hazelcast.core.QueueStore;
import com.hazelcast.core.QueueStoreFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Properties;

public class MongoQueueStorageFactory implements QueueStoreFactory {

    private MongoTemplate mongoTemplate;

    public MongoQueueStorageFactory(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public QueueStore newQueueStore(String name, Properties properties) {
        return new MongoQueueStore(name, mongoTemplate);
    }
}
