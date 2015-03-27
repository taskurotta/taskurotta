package ru.taskurotta.service.hz.support;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.mongodb.driver.*;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.storage.InterruptedTasksService;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Should restore interrupted tasks previously stored in mongoDB to hz IMap on startup
 * Created on 24.03.2015.
 */
public class HzInterruptedTasksRestoreSupport {

    private static final Logger logger = LoggerFactory.getLogger(HzInterruptedTasksRestoreSupport.class);

    private HazelcastInstance hzInstance;
    private DB mongoDb;
    private boolean restore = true;
    private String mapName;
    private int batchSize = 100;
    private DBCollection dbCollection;
    private InterruptedTasksService interruptedTasksService;

    BSerializationService serializationService;

    public void init() {
        dbCollection = mongoDb.getCollection(mapName);
        StreamBSerializer objectSerializer = serializationService.getSerializer(InterruptedTask.class.getName());
        dbCollection.setDBDecoderFactory(new BDecoderFactory(objectSerializer));
        dbCollection.setDBEncoderFactory(new BEncoderFactory(serializationService));

        if (restore) {

            ExecutorService service = Executors.newFixedThreadPool(1);
            service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean done = false;
                        while (!done) {
                            try {
                                long restored = restore();
                                done = true;
                                logger.info("Restored [{}] interrupted tasks", restored);
                            } catch (Exception e) {
                                logger.warn("Error while restoring interrupted tasks from mongo", e);
                                TimeUnit.SECONDS.sleep(10);
                            }
                        }
                    } catch (InterruptedException e) {
                        // interrupted, go out
                    }
                }
            });
        } else {
            logger.warn("Interrupted tasks restoration from map store on startup is disabled");
        }
    }

    private long restore() {
        long restored = 0l;
        ILock restorationLock = hzInstance.getLock(HzInterruptedTasksRestoreSupport.class.getName());
        if (restorationLock.tryLock()) {
            try {
                IMap<UUID, InterruptedTask> hzCollection = hzInstance.getMap(mapName);
                long dbObjects = dbCollection.count();
                long hzObjects = hzCollection.size();
                if (dbObjects > hzObjects) {
                    logger.info("DbStore contains more tasks[{}] then map does[{}]: try restore...", dbObjects, hzObjects);
                    long batchesCount = dbObjects/batchSize;
                    if (dbObjects%batchSize > 0) {
                        batchesCount++;
                    }
                    for (int i = 0; i<batchesCount; i++) {
                        Set<InterruptedTask> batch = getBatchToLoad(dbCollection, i);
                        logger.debug("Try to load interrupted tasks batch batch [{}] with size[{}]", i, batch.size());
                        for (InterruptedTask it : batch) {
                            interruptedTasksService.save(it);
                        }
                        restored += batch.size();
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("After restoration: dbTasks[{}] hzTasks[{}]", dbCollection.count(), hzCollection.size());
                    }

                }
            } finally {
                restorationLock.unlock();
            }
        }
        return restored;
    }

    private Set<InterruptedTask> getBatchToLoad(DBCollection dbCollection, int batchNumber) {
        Set<InterruptedTask> result = new HashSet<>();
        DBCursor cursor = dbCollection.find().skip(batchSize*batchNumber).limit(batchSize);
        while (cursor.hasNext()) {
            DBObject dbObj = cursor.next();
            if (dbObj instanceof DBObjectCheat) {
                InterruptedTask task  = ((DBObjectCheat<InterruptedTask>) dbObj).getObject();
                if (task != null) {
                    result.add(task);
                }
            }
        }
        return result;
    }

    @Required
    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    @Required
    public void setMongoDb(DB mongoDb) {
        this.mongoDb = mongoDb;
    }

    @Required
    public void setRestore(boolean restore) {
        this.restore = restore;
    }

    @Required
    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    @Required
    public void setSerializationService(BSerializationService serializationService) {
        this.serializationService = serializationService;
    }

    @Required
    public void setInterruptedTasksService(InterruptedTasksService interruptedTasksService) {
        this.interruptedTasksService = interruptedTasksService;
    }
}
