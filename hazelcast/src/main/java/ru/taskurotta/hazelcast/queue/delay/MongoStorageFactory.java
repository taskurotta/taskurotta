package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.spring.mongodb.MongoDBConverter;
import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;
import java.util.Set;
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

    private MongoDBConverter converter;

    private transient final ReentrantLock lock = new ReentrantLock();
    private ConcurrentHashMap<String, String> dbCollectionNamesMap = new ConcurrentHashMap<>();

    public static final String OBJECT_NAME = "object";
    public static final String ENQUEUE_TIME_NAME = "enqueueTime";

    public MongoStorageFactory(final HazelcastInstance hazelcastInstance, final MongoTemplate mongoTemplate,
                               String storagePrefix, long scheduleDelayMillis) {
        this.mongoTemplate = mongoTemplate;
        this.storagePrefix = storagePrefix;

        this.converter = new SpringMongoDBConverter(mongoTemplate);

        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        for (String collectionName : collectionNames) {
            if (collectionName.startsWith(storagePrefix)) {
                String queueName = collectionName.replaceFirst(storagePrefix, "");
                dbCollectionNamesMap.put(queueName, collectionName);
            }
        }

        ScheduledExecutorService singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("MongoStorageFactory-" + counter++);
                thread.setDaemon(true);
                return thread;
            }
        });

        singleThreadScheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    Set<Map.Entry<String, String>> entrySet = dbCollectionNamesMap.entrySet();

                    BasicDBObject query = new BasicDBObject(ENQUEUE_TIME_NAME, new BasicDBObject("$lte", System.currentTimeMillis()));

                    for (Map.Entry<String, String> entry : entrySet) {
                        String dbCollectionName = entry.getValue();
                        DBCollection dbCollection = mongoTemplate.getCollection(dbCollectionName);

                        String queueName = entry.getKey();
                        IQueue iQueue = hazelcastInstance.getQueue(queueName);

                        try (DBCursor dbCursor = dbCollection.find(query)) {
                            while (dbCursor.hasNext()) {
                                DBObject dbObject = dbCursor.next();
                                StorageItem storageItem = (StorageItem) converter.toObject(StorageItem.class, dbObject);

                                if (iQueue.offer(storageItem.getObject())) {
                                    dbCollection.remove(dbObject);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        }, 0l, scheduleDelayMillis, TimeUnit.MILLISECONDS);
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
                dbCollection.createIndex(new BasicDBObject(ENQUEUE_TIME_NAME, 1));
            } finally {
                lock.unlock();
            }
        }

        final DBCollection dbCollection = mongoTemplate.getCollection(dbCollectionName);
        final String finalDbCollectionName = dbCollectionName;

        return new Storage() {
            @Override
            public boolean add(Object o, long delayTime, TimeUnit unit) {
                return save(o, delayTime, unit, dbCollection, queueName);
            }

            @Override
            public boolean remove(Object o) {
                return delete(o, dbCollection);
            }

            @Override
            public void clear() {
                dbCollection.drop();
            }

            @Override
            public void destroy() {
                String unPrefixed = removePrefix(finalDbCollectionName);
                dbCollectionNamesMap.remove(queueName);

                mongoTemplate.dropCollection(finalDbCollectionName);
                mongoTemplate.dropCollection(unPrefixed);

                logger.debug("Destroying storage collections: delayedStore[{}] and queueBackingStore[{}]", finalDbCollectionName, unPrefixed);
            }
        };
    }

    private String removePrefix (String target) {
        String result = target;
        if (target != null && storagePrefix!= null && target.startsWith(storagePrefix)) {
            result = target.substring(storagePrefix.length(), target.length());
        }
        return result;
    }

    private boolean save(Object o, long delayTime, TimeUnit unit, DBCollection dbCollection, String queueName) {
        long enqueueTime = System.currentTimeMillis() + unit.toMillis(delayTime);

        DBObject dbObject = converter.toDBObject(new StorageItem(o, enqueueTime, queueName));

        dbCollection.save(dbObject);

        return true;
    }

    private boolean delete(Object o, DBCollection dbCollection) {
        dbCollection.remove(new BasicDBObject(OBJECT_NAME, o));
        return true;
    }
}
