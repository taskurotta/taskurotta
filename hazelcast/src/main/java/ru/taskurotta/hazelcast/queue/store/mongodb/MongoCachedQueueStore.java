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
import ru.taskurotta.hazelcast.queue.store.mongodb.bson.QueueItemContainerStreamBSerializer;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.DBObjectСheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.mongodb.driver.impl.BDecoderFactory;
import ru.taskurotta.mongodb.driver.impl.BEncoderFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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

    private String objectClassName;

    private int batchSize;

    public MongoCachedQueueStore(String storageName, MongoTemplate mongoTemplate, CachedQueueStoreConfig config,
                                 BSerializationService serializationService) {
        this.storageName = storageName;
        this.mongoTemplate = mongoTemplate;
        this.batchSize = config.getBatchLoadSize();
        this.coll = mongoTemplate.getCollection(this.storageName);
        this.converter = new SpringMongoDBConverter(mongoTemplate);

        // todo: throw exception if it is null
        if (serializationService == null) {
            return;
        }

        objectClassName = config.getObjectClassName();
        if (objectClassName == null) {
            logger.warn("Name of object class not found in queue store config. Storage name is" +
                    " [" + storageName + "]. Queue are in legacy mode...");
            return;
        }


        StreamBSerializer objectSerializer = serializationService.getSerializer(objectClassName);
        QueueItemContainerStreamBSerializer containerStreamBSerializer = new QueueItemContainerStreamBSerializer
                (objectSerializer);


        coll.setDBDecoderFactory(new BDecoderFactory(containerStreamBSerializer));
        coll.setDBEncoderFactory(new BEncoderFactory(containerStreamBSerializer));
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
    public void store(Long id, Object taskQueueItem) {
        long startTime = System.nanoTime();

        try {

            if (objectClassName == null) {
                DBObject dbo = converter.toDBObject(taskQueueItem);
                dbo.put("_id", id);
                coll.save(dbo);
            } else {
                QueueItemContainer queueItemContainer = new QueueItemContainer();
                queueItemContainer.setId(id);
                queueItemContainer.setQueueItem(taskQueueItem);

                DBObjectСheat document = new DBObjectСheat(queueItemContainer);

                coll.insert(document);
            }
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

    // todo: should be removed
    @Override
    public Object load(Long aLong) {
        long startTime = System.nanoTime();

        try {


            DBObject dbo = new BasicDBObject();
            dbo.put("_id", aLong);
            DBObject obj = coll.findOne(dbo);

            if (obj == null)
                return null;

            if (objectClassName == null) {
                try {
                    Class clazz = Class.forName(obj.get("_class").toString());
                    return converter.toObject(clazz, obj);
                } catch (ClassNotFoundException e) {
                    logger.error(e.getMessage(), e);
                }
                return null;

            } else {
                return ((QueueItemContainer) ((DBObjectСheat) obj).getObject()).getQueueItem();
            }

        } finally {
            loadTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }


    @Override
    public Map<Long, Object> loadAll(long from, long to) {

        long startTime = System.nanoTime();

        Map<Long, Object> map = new HashMap<>();

        try {

            BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", from).append("$lte", to));
            try (DBCursor cursor = coll.find(query).batchSize(batchSize)) {
                while (cursor.hasNext()) {

                    DBObject obj = cursor.next();

                    if (objectClassName == null) {
                        try {
                            Class clazz = Class.forName(obj.get("_class").toString());
                            map.put((Long) obj.get("_id"), converter.toObject(clazz, obj));
                        } catch (ClassNotFoundException e) {
                            logger.error(e.getMessage(), e);
                        }
                    } else {
                        QueueItemContainer queueItemContainer = (QueueItemContainer) ((DBObjectСheat) obj).getObject();
                        map.put(queueItemContainer.getId(), queueItemContainer.getQueueItem());
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
        long result = (asc) ? 0 : -1;

        final DBObject sortCommand = new BasicDBObject();
        sortCommand.put("_id", (asc) ? 1 : -1);
        final DBObject val;

        try (DBCursor cursor = coll.find().sort(sortCommand).limit(1)) {
            if (cursor.hasNext() && (val = cursor.next()) != null) {
                if (objectClassName == null) {
                    return (Long) val.get("_id");
                } else {
                    return ((QueueItemContainer) ((DBObjectСheat) val).getObject()).getId();
                }
            }
        }
        return result;
    }

}
