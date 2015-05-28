package ru.taskurotta.hazelcast.store;

import com.hazelcast.core.HazelcastInstance;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;

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

    private DB mongoDB;
    private String queuePrefix;
    private boolean restore = true;
    private HazelcastInstance hzInstance;

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

    public void setMongoDB(DB mongoDB) {
        this.mongoDB = mongoDB;
    }

    public void setQueuePrefix(String queuePrefix) {
        this.queuePrefix = queuePrefix;
    }

    public void setRestore(boolean restore) {
        this.restore = restore;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    private void restore() {
        int queueRestored = 0;
        for (String collectionName : mongoDB.getCollectionNames()) {
            if (collectionName.startsWith(queuePrefix)) {//is backing queue

                // initialize cached queue
                CachedQueueServiceConfig.getCachedQueue(hzInstance, collectionName);

                if (logger.isDebugEnabled()) {
                    DBCollection coll = mongoDB.getCollection(collectionName);
                    logger.debug("Restoring queue [{}] with [{}] HZ elements and [{}] mongo elements", collectionName, coll.getCount());
                }

                queueRestored++;
            }
        }
        logger.info("Restored [{}] MongoDB persisted queues", queueRestored);
    }
}
