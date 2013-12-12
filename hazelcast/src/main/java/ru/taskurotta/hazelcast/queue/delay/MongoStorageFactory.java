package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: stukushin
 * Date: 10.12.13
 * Time: 16:58
 */
public class MongoStorageFactory implements StorageFactory {

    private static final Logger logger = LoggerFactory.getLogger(MongoStorageFactory.class);

    private MongoTemplate mongoTemplate;
    private String storagePrefix;

    private transient final ReentrantLock lock = new ReentrantLock();
    private ConcurrentHashMap<String, String> dbCollectionNamesMap = new ConcurrentHashMap<>();

    public MongoStorageFactory(final HazelcastInstance hazelcastInstance, final MongoTemplate mongoTemplate,
                               String storagePrefix, String schedule) {
        this.mongoTemplate = mongoTemplate;
        this.storagePrefix = storagePrefix;

        long delay = 1000l;
        TimeUnit delayTimeUnit = TimeUnit.MILLISECONDS;
        String[] params = schedule.split("_");
        if (params.length == 2) {
            delay = Long.valueOf(params[0]);
            delayTimeUnit = TimeUnit.valueOf(params[1].toUpperCase());
        }
        logger.info("Set schedule delay = [{}] delayTimeUnit = [{}] for search ready processes for GC", delay, delayTimeUnit);

        ScheduledExecutorService singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("MongoStorageFactory-" + counter++);
                return thread;
            }
        });

        singleThreadScheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, String> entry : dbCollectionNamesMap.entrySet()) {
                    String dbCollectionName = entry.getValue();

                    DBCollection dbCollection = mongoTemplate.getCollection(dbCollectionName);

                    BasicDBObject query = new BasicDBObject(MongoStorage.ENQUEUE_TIME_NAME,
                            new BasicDBObject("$lte", System.currentTimeMillis()));

                    try (DBCursor dbCursor = dbCollection.find(query)) {

                        if (dbCursor.size() == 0) {
                            continue;
                        }

                        String queueName = entry.getKey();
                        IQueue iQueue = hazelcastInstance.getQueue(queueName);

                        while (dbCursor.hasNext()) {
                            DBObject dbObject = dbCursor.next();
                            if (iQueue.add(dbObject.get(MongoStorage.OBJECT_NAME))) {
                                dbCollection.remove(dbObject);
                            }
                        }
                    }
                }
            }
        }, 0l, delay, delayTimeUnit);
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
