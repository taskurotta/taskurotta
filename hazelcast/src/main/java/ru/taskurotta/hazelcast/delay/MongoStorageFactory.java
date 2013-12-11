package ru.taskurotta.hazelcast.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: stukushin
 * Date: 10.12.13
 * Time: 16:58
 */
public class MongoStorageFactory implements StorageFactory {

    private MongoTemplate mongoTemplate;
    private String storagePrefix;

    private transient final ReentrantLock lock = new ReentrantLock();
    private ConcurrentHashMap<String, String> dbCollectionNamesMap = new ConcurrentHashMap<>();

    public MongoStorageFactory(final HazelcastInstance hazelcastInstance, final MongoTemplate mongoTemplate,
                               int poolSize, String storagePrefix) {
        this.mongoTemplate = mongoTemplate;
        this.storagePrefix = storagePrefix;

        ExecutorService executorService = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("MongoStorageFactory-" + counter++);
                return thread;
            }
        });

        for (int i = 0; i < poolSize; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        for (Map.Entry<String, String> entry : dbCollectionNamesMap.entrySet()) {
                            String dbCollectionName = entry.getValue();

                            DBCollection dbCollection = mongoTemplate.getCollection(dbCollectionName);

                            if (dbCollection.count() == 0) {
                                continue;
                            }

                            BasicDBObject query = new BasicDBObject(MongoStorage.ENQUEUE_TIME_NAME,
                                    new BasicDBObject("$lte", System.currentTimeMillis()));

                            try (DBCursor dbCursor = dbCollection.find(query)) {

                                if (dbCursor.size() == 0) {
                                    continue;
                                }

                                String queueName = entry.getKey();
                                IQueue iQueue = hazelcastInstance.getQueue(queueName);

                                while(dbCursor.hasNext()) {
                                    DBObject dbObject = dbCursor.next();
                                    iQueue.add(dbObject.get(MongoStorage.OBJECT_NAME));
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public Storage createStorage(final String queueName) {

        String dbCollectionName = dbCollectionNamesMap.get(queueName);

        if (dbCollectionName == null) {

            final ReentrantLock lock = this.lock;
            lock.lock();

            try {

                dbCollectionName = dbCollectionNamesMap.get(queueName);
                if (dbCollectionName == null) {
                    dbCollectionName = storagePrefix + queueName;
                }
                dbCollectionNamesMap.put(queueName, dbCollectionName);

                DBCollection dbCollection = mongoTemplate.getCollection(dbCollectionName);
                dbCollection.createIndex(new BasicDBObject(MongoStorage.ENQUEUE_TIME_NAME, 1));
            } finally {
                lock.unlock();
            }
        }

        DBCollection dbCollection = mongoTemplate.getCollection(dbCollectionName);

        return new MongoStorage(dbCollection);
    }
}
