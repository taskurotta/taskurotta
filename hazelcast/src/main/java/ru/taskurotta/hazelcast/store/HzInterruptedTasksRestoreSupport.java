package ru.taskurotta.hazelcast.store;

import com.hazelcast.core.HazelcastInstance;
import com.mongodb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

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

    public void init() {
        if (restore) {
            //TODO: implement mongo stored keys load to IMap. Such behaviour does not provided automaticaly due to empty MongoMapStore#loadAllKeys() optimization

        } else {
            logger.warn("Interrupted tasks restoration from map store on startup is disabled");
        }
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
}
