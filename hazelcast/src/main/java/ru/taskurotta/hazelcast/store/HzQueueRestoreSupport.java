package ru.taskurotta.hazelcast.store;

import com.hazelcast.core.HazelcastInstance;
import com.mongodb.DBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;

/**
 * Support for  persisted in mongoDB queues restore.
 * Initializes persisted queues after node start to HZ memory
 * User: dimadin
 * Date: 13.08.13 15:55
 */
public class HzQueueRestoreSupport {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueRestoreSupport.class);
    private static final String BACKING_MAP_SUFFIX = ".backingMap";

    private MongoTemplate mongoTemplate;
    private HazelcastInstance hzInstance;
    private String queuePrefix;
    private HzQueueConfigSupport hzQueueConfigSupport;
    private boolean restore = true;

    public void init() {
        if (restore) {
            int queueRestored = 0;
            for (String collectionName : mongoTemplate.getCollectionNames()) {
                if (collectionName.startsWith(queuePrefix)) {//is backing queue
                    String queueName = collectionName.endsWith(BACKING_MAP_SUFFIX) ? collectionName.substring(0, collectionName.lastIndexOf(BACKING_MAP_SUFFIX)) : collectionName;
                    if (hzQueueConfigSupport != null) {
                        hzQueueConfigSupport.createQueueConfig(queueName);
                    } else {
                        logger.warn("Cannot restore queue[{}], mapstore config is not set!", queueName);
                    }
                    int size = hzInstance.getQueue(queueName).size();//initializes queue

                    if (logger.isDebugEnabled()) {
                        DBCollection coll = mongoTemplate.getCollection(collectionName);
                        logger.debug("Restoring queue [{}] with [{}] HZ elements and [{}] mongo elements", queueName, size, coll.getCount());
                    }

                    queueRestored++;
                }
            }
            logger.info("Restored [{}] MongoDB persisted queues", queueRestored);
        } else {
            logger.info("Queue restoration from map store on startup is disabled");
        }
    }


    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
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
}
