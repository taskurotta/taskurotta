package ru.taskurotta.hazelcast;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hazelcast.core.QueueStore;
import com.hazelcast.spring.mongodb.MongoDBConverter;
import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.backend.queue.TaskQueueItem;

/**
 * User: moroz
 * Date: 16.08.13
 */
public class MongoQueueStore implements QueueStore<TaskQueueItem> {

    private String storageName;
    private MongoTemplate mongoTemplate;
    private MongoDBConverter converter;
    private DBCollection coll;

    protected static final Logger logger = Logger.getLogger(MongoQueueStore.class.getName());

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
        DBObject dbo = new BasicDBObject();
        dbo.put("_id", aLong);
        coll.remove(dbo);
    }

    @Override
    public void store(Long aLong, TaskQueueItem taskQueueItem) {
        DBObject dbo = converter.toDBObject(taskQueueItem);
        dbo.put("_id", aLong);
        coll.save(dbo);
    }

    @Override
    public void storeAll(Map<Long, TaskQueueItem> longTaskQueueItemMap) {
        for (Map.Entry<Long, TaskQueueItem> entry : longTaskQueueItemMap.entrySet()) {
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
    public TaskQueueItem load(Long aLong) {
        DBObject dbo = new BasicDBObject();
        dbo.put("_id", aLong);
        DBObject obj = coll.findOne(dbo);
        if (obj == null)
            return null;

        try {
            Class clazz = Class.forName(obj.get("_class").toString());
            return (TaskQueueItem) converter.toObject(clazz, obj);
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Map<Long, TaskQueueItem> loadAll(Collection<Long> longs) {
        Map<Long, TaskQueueItem> map = new HashMap<Long, TaskQueueItem>();
        BasicDBList dbo = new BasicDBList();
        for (Long key : longs) {
            dbo.add(new BasicDBObject("_id", key));
        }
        BasicDBObject dbb = new BasicDBObject("$or", dbo);
        DBCursor cursor = coll.find(dbb);
        while (cursor.hasNext()) {
            try {
                DBObject obj = cursor.next();
                Class clazz = null;
                clazz = Class.forName(obj.get("_class").toString());
                map.put((Long) obj.get("_id"), (TaskQueueItem) converter.toObject(clazz, obj));
            } catch (ClassNotFoundException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return map;
    }

    @Override
    public Set<Long> loadAllKeys() {
        Set<Long> keyset = new HashSet<Long>();
        BasicDBList dbo = new BasicDBList();
        dbo.add("_id");
        DBCursor cursor = coll.find(null, dbo);
        while (cursor.hasNext()) {
            keyset.add((Long) cursor.next().get("_id"));
        }
        return keyset;
    }
}
