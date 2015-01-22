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
import ru.taskurotta.service.recovery.IncompleteProcessesCursor;

import java.io.IOException;
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
    public IncompleteProcessesCursor findProcesses(long timeBefore, final int batchSize) {

        final DBCollection dbCollection = mongoTemplate.getCollection(processesStorageMapName);

        BasicDBObject query = new BasicDBObject();
        query.append(START_TIME_INDEX_NAME, new BasicDBObject("$lte", timeBefore));
        query.append(STATE_INDEX_NAME, Process.START);

        return new MongoIncompleteProcessesCursor(dbCollection, query, batchSize);
    }

    private class MongoIncompleteProcessesCursor implements IncompleteProcessesCursor {

        DBCollection dbCollection;
        BasicDBObject query;
        int batchSize;

        DBCursor dbCursor;

        public MongoIncompleteProcessesCursor(DBCollection dbCollection, BasicDBObject query, int batchSize) {
            this.dbCollection = dbCollection;
            this.query = query;
            this.batchSize = batchSize;
        }

        public void open() {
            dbCursor = dbCollection.find(query).batchSize(batchSize);
        }

        @Override
        public void close() throws IOException {
            dbCursor.close();
        }

        @Override
        public Collection<UUID> getNext() {

            if (dbCursor == null) {
                open();
            }

            Collection<UUID> result = new ArrayList<>();

            int i = 0;
            while (i < batchSize && dbCursor.hasNext()) {
                DBObject dbObject = dbCursor.next();
                Process process = (Process) converter.toObject(Process.class, dbObject);
                UUID processId = process.getProcessId();
                result.add(processId);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Found [{}] incomplete processes", result.size());
            }

            return result;
        }
    }

}
