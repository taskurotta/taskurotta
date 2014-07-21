package ru.taskurotta.hazelcast.store;

import com.hazelcast.core.QueueStore;
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
import ru.taskurotta.hazelcast.ItemIdAware;

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
public class MongoQueueStore implements QueueStore<Object>, ItemIdAware {

    protected static final Logger logger = LoggerFactory.getLogger(MongoQueueStore.class);
    public static Timer storeTimer = Metrics.newTimer(MongoQueueStore.class, "store",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    public static Timer loadTimer = Metrics.newTimer(MongoQueueStore.class, "load",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    public static Timer deleteTimer = Metrics.newTimer(MongoQueueStore.class, "delete",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    private String storageName;
    private MongoTemplate mongoTemplate;
    private MongoDBConverter converter;
    private DBCollection coll;

    public MongoQueueStore(String storageName, MongoTemplate mongoTemplate) {
        this.storageName = storageName;
        this.mongoTemplate = mongoTemplate;
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
        Map<Long, Object> map = new HashMap<>();
        BasicDBList dbo = new BasicDBList();
        for (Long key : longs) {
            dbo.add(new BasicDBObject("_id", key));
        }
        BasicDBObject dbb = new BasicDBObject("$or", dbo);
        DBCursor cursor = coll.find(dbb);
        while (cursor.hasNext()) {
            try {
                DBObject obj = cursor.next();
                Class clazz = Class.forName(obj.get("_class").toString());
                map.put((Long) obj.get("_id"), converter.toObject(clazz, obj));
            } catch (ClassNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }

//        duplicates check in QueueContainer#load
//        if (map.size() != longs.size()) {
//            logger.warn("Cannot load all items from store, loaded [{}] out of [{}]", map.size(), longs.size());
//        }

        return map;
    }

    /**
     * @return min Id of an item stored
     */
    public long getMinItemId() {
        long result = -1l;

        DBObject sortCommand = new BasicDBObject();
        sortCommand.put("_id", 1);//by _id asc(min first)

        DBObject val;
        DBCursor cursor = coll.find().sort(sortCommand).limit(1);
        if (cursor.hasNext() && (val = cursor.next()) != null) {
            result = Long.valueOf(val.get("_id").toString());
        }

        return result;
    }

    @Override
    public Set<Long> loadAllKeys() {
        Set<Long> keySet = new HashSet<>();
        BasicDBList dbo = new BasicDBList();
        dbo.add("_id");
        DBCursor cursor = coll.find(null, dbo);
        while (cursor.hasNext()) {
            keySet.add((Long) cursor.next().get("_id"));
        }
        return keySet;
    }
}
