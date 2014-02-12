package ru.taskurotta.service.hz.recovery;

import com.hazelcast.spring.mongodb.MongoDBConverter;
import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.recovery.IncompleteProcessDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Date: 13.01.14 13:03
 */
public class MongoIncompleteProcessDao implements IncompleteProcessDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoIncompleteProcessDao.class);

    private static final String START_TIME_INDEX_NAME = "startTime";
    private static final String STATE_INDEX_NAME = "state";

    private MongoDBConverter converter;
    private MongoTemplate mongoTemplate;
    private String processesStorageMapName;

    public MongoIncompleteProcessDao(MongoTemplate mongoTemplate, String processesStorageMapName) {
        this.mongoTemplate = mongoTemplate;
        this.processesStorageMapName = processesStorageMapName;
        this.converter = new SpringMongoDBConverter(mongoTemplate);
    }

    @Override
    public Collection<UUID> findProcesses(long timeBefore) {
        Collection<UUID> result = new ArrayList<>();

        DBCollection dbCollection = mongoTemplate.getCollection(processesStorageMapName);

        BasicDBObject query = new BasicDBObject();
        query.append(START_TIME_INDEX_NAME, new BasicDBObject("$lte", timeBefore));
        query.append(STATE_INDEX_NAME, Process.START);

        try (DBCursor dbCursor = dbCollection.find(query)) {
            while (dbCursor.hasNext()) {
                DBObject dbObject = dbCursor.next();
                Process process = (Process) converter.toObject(Process.class, dbObject);
                UUID processId = process.getProcessId();
                result.add(processId);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Found [{}] incomplete processes", result.size());
        }

        return result;
    }
}
