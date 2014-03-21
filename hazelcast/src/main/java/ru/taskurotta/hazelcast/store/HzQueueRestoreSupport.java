package ru.taskurotta.hazelcast.store;

import com.mongodb.DBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Support for  persisted in mongoDB queues restore.
 * Initializes persisted queues after node start to HZ memory
 * User: dimadin
 * Date: 13.08.13 15:55
 */
public class HzQueueRestoreSupport {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueRestoreSupport.class);

    private MongoTemplate mongoTemplate;
    private String queuePrefix;
    private HzQueueConfigSupport hzQueueConfigSupport;
    private boolean restore = true;

    public void init() {
        if (restore) {
            ExecutorService service = Executors.newFixedThreadPool(1);
            service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean done = false;
                        while (!done) {
                            try {
                                restore();
                                done = true;
                            } catch (Exception e) {
                                logger.warn("Error while restoring queue config from mongo", e);
                                TimeUnit.SECONDS.sleep(10);
                            }
                        }
                    } catch (InterruptedException e) {
                        // interrupted, go out
                    }
                }
            });
        } else {
            logger.info("Queue restoration from map store on startup is disabled");
        }
    }

    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void setQueuePrefix(String queuePrefix) {
        this.queuePrefix = queuePrefix;
    }

    public void setHzQueueConfigSupport(HzQueueConfigSupport hzQueueConfigSupport) {
        this.hzQueueConfigSupport = hzQueueConfigSupport;
    }

    public void setRestore(boolean restore) {
        this.restore = restore;
    }

    private void restore() {
        int queueRestored = 0;
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            if (collectionName.startsWith(queuePrefix)) {//is backing queue
                if (hzQueueConfigSupport != null) {
                    hzQueueConfigSupport.createQueueConfig(collectionName);
                } else {
                    logger.warn("Cannot restore queue[{}], mapstore config is not set!", collectionName);
                }

                if (logger.isDebugEnabled()) {
                    DBCollection coll = mongoTemplate.getCollection(collectionName);
                    logger.debug("Restoring queue [{}] with [{}] HZ elements and [{}] mongo elements", collectionName, coll.getCount());
                }

                queueRestored++;
            }
        }
        logger.info("Restored [{}] MongoDB persisted queues", queueRestored);
    }
}
