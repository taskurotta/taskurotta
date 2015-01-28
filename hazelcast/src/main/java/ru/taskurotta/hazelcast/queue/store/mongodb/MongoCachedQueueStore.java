package ru.taskurotta.hazelcast.queue.store.mongodb;

import com.hazelcast.spring.mongodb.MongoDBConverter;
import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * User: moroz
 * Date: 16.08.13
 */
public class MongoCachedQueueStore implements CachedQueueStore<Object> {

    protected static final Logger logger = LoggerFactory.getLogger(MongoCachedQueueStore.class);

    public static Timer storeTimer = Metrics.newTimer(MongoCachedQueueStore.class, "store",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    public static Timer loadTimer = Metrics.newTimer(MongoCachedQueueStore.class, "load",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    public static Timer loadAllTimer = Metrics.newTimer(MongoCachedQueueStore.class, "loadAll",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    public static Timer deleteTimer = Metrics.newTimer(MongoCachedQueueStore.class, "delete",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    private String storageName;
    private MongoTemplate mongoTemplate;
    private MongoDBConverter converter;
    private DBCollection coll;

    private int batchSize;

    public MongoCachedQueueStore(String storageName, MongoTemplate mongoTemplate, CachedQueueStoreConfig config) {
        this.storageName = storageName;
        this.mongoTemplate = mongoTemplate;
        this.batchSize = config.getBatchLoadSize();
        this.coll = mongoTemplate.getCollection(this.storageName);
        this.converter = new SpringMongoDBConverter(mongoTemplate);
    }

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    public String getStorageName() {
        return storageName;
    }

    @Override
    public void delete(Long aLong) {
        long startTime = System.nanoTime();

        try {
            DBObject dbo = new BasicDBObject();
            dbo.put("_id", aLong);
            coll.remove(dbo);
        } finally {
            deleteTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void store(Long aLong, Object taskQueueItem) {
        long startTime = System.nanoTime();

        try {
            DBObject dbo = converter.toDBObject(taskQueueItem);
            dbo.put("_id", aLong);
            coll.save(dbo);
        } finally {
            storeTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void storeAll(Map<Long, Object> longTaskQueueItemMap) {
        for (Map.Entry<Long, Object> entry : longTaskQueueItemMap.entrySet()) {
            store(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void deleteAll(Collection<Long> longs) {
        BasicDBList dbo = new BasicDBList();
        for (Long key : longs) {
            dbo.add(new BasicDBObject("_id", key));
        }
        BasicDBObject dbb = new BasicDBObject("$or", dbo);
        coll.remove(dbb);
    }

    @Override
    public void clear() {
        coll.drop();
    }

    @Override
    public Object load(Long aLong) {
        long startTime = System.nanoTime();

        try {

            DBObject dbo = new BasicDBObject();
            dbo.put("_id", aLong);
            DBObject obj = coll.findOne(dbo);
            if (obj == null)
                return null;

            try {
                Class clazz = Class.forName(obj.get("_class").toString());
                return converter.toObject(clazz, obj);
            } catch (ClassNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
            return null;

        } finally {
            loadTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }


    @Override
    public Map<Long, Object> loadAll(Collection<Long> longs) {

        long startTime = System.nanoTime();

        Map<Long, Object> map = new HashMap<>();

        try {

            BasicDBList dbo = new BasicDBList();
            for (Long key : longs) {
                dbo.add(new BasicDBObject("_id", key));
            }
            BasicDBObject dbb = new BasicDBObject("$or", dbo);
            try (DBCursor cursor = coll.find(dbb).batchSize(batchSize)) {
                while (cursor.hasNext()) {
                    try {
                        DBObject obj = cursor.next();
                        Class clazz = Class.forName(obj.get("_class").toString());
                        map.put((Long) obj.get("_id"), converter.toObject(clazz, obj));
                    } catch (ClassNotFoundException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }

        } finally {
            loadAllTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }

        return map;
    }


    @Override
    public Map<Long, Object> loadAll(long from, long to) {

        long startTime = System.nanoTime();

        Map<Long, Object> map = new HashMap<>();

        try {

            BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", from).append("$lte", to));
            try (DBCursor cursor = coll.find(query).batchSize(batchSize)) {
                while (cursor.hasNext()) {
                    try {
                        DBObject obj = cursor.next();
                        Class clazz = Class.forName(obj.get("_class").toString());
                        map.put((Long) obj.get("_id"), converter.toObject(clazz, obj));
                    } catch (ClassNotFoundException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }

        } finally {
            loadAllTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }

        return map;
    }

    /**
     * @return min Id of the stored items
     */
    public long getMinItemId() {
        return getFirstItemIdByAscDesc(true);
    }

    /**
     * @return max Id of the stored items
     */
    public long getMaxItemId() {
        return getFirstItemIdByAscDesc(false);
    }

    private long getFirstItemIdByAscDesc(boolean asc) {
        long result = -1l;
        final DBObject sortCommand = new BasicDBObject();
        sortCommand.put("_id", (asc) ? 1 : -1);
        final DBObject val;
        try (DBCursor cursor = coll.find().sort(sortCommand).limit(1)) {
            if (cursor.hasNext() && (val = cursor.next()) != null) {
                result = (Long) val.get("_id");
            }
        }
        return result;
    }

    @Override
    public Set<Long> loadAllKeys() {
        Set<Long> keySet = new HashSet<>();
        BasicDBList dbo = new BasicDBList();
        dbo.add("_id");
        try (DBCursor cursor = coll.find(null, dbo)) {
            while (cursor.hasNext()) {
                keySet.add((Long) cursor.next().get("_id"));
            }
        }
        return keySet;
    }
}
