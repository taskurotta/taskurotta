package ru.taskurotta.hazelcast.queue.store.mongodb;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;
import ru.taskurotta.hazelcast.queue.store.mongodb.bson.QueueItemContainerStreamBSerializer;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.BSerializationServiceFactory;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.mongodb.driver.BDecoderFactory;
import ru.taskurotta.mongodb.driver.BEncoderFactory;

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

    private static WriteConcern noWaitWriteConcern = new WriteConcern(0, 0, false, true);

    private String storageName;
    private MongoTemplate mongoTemplate;
    private DBCollection coll;

    private String objectClassName;

    private int batchSize;

    public MongoCachedQueueStore(String storageName, MongoTemplate mongoTemplate, CachedQueueStoreConfig config,
                                 BSerializationService serializationService) {

        this.storageName = storageName;
        this.mongoTemplate = mongoTemplate;
        this.batchSize = config.getBatchLoadSize();
        this.coll = mongoTemplate.getCollection(this.storageName);

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

        BSerializationService mainBSerializationService = BSerializationServiceFactory.newInstance
                (serializationService, containerStreamBSerializer);

        coll.setDBDecoderFactory(new BDecoderFactory(containerStreamBSerializer));
        coll.setDBEncoderFactory(new BEncoderFactory(mainBSerializationService));
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
            coll.remove(dbo, noWaitWriteConcern);
        } finally {
            deleteTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void store(Long id, Object taskQueueItem) {
        long startTime = System.nanoTime();

        try {

            QueueItemContainer queueItemContainer = new QueueItemContainer();
            queueItemContainer.setId(id);
            queueItemContainer.setQueueItem(taskQueueItem);

            DBObjectCheat<QueueItemContainer> document = new DBObjectCheat<>(queueItemContainer);

            coll.insert(document);
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

        int j = 0;
        int size = longs.size();

        BasicDBList idList = new BasicDBList();

        for (long id : longs) {
            j++;

            idList.add(id);

            if (j % 100 == 0 && j == size) {
                BasicDBObject inListObj = new BasicDBObject("$in", idList);
                coll.remove(new BasicDBObject("_id", inListObj), noWaitWriteConcern);
                idList.clear();
                ;
            }
        }

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

            if (obj == null) {
                return null;
            }

            return ((QueueItemContainer) ((DBObjectCheat) obj).getObject()).getQueueItem();

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

                    QueueItemContainer queueItemContainer = (QueueItemContainer) ((DBObjectCheat) obj).getObject();
                    map.put(queueItemContainer.getId(), queueItemContainer.getQueueItem());
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
                return ((QueueItemContainer) ((DBObjectCheat) val).getObject()).getId();
            }
        }
        return result;
    }

}
