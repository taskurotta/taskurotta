package ru.taskurotta.service.hz.recovery;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.hz.serialization.bson.ProcessBSerializer;
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

    private static final String START_TIME_INDEX_NAME = ProcessBSerializer.START_TIME.toString();
    private static final String STATE_INDEX_NAME = ProcessBSerializer.STATE.toString();

    private final MongoTemplate mongoTemplate;
    private final String processesStorageMapName;
    private final DBCollection dbCollection;

    public MongoIncompleteProcessDao(MongoTemplate mongoTemplate, String processesStorageMapName,
                                     BSerializationService bSerializationService) {
        this.mongoTemplate = mongoTemplate;
        this.processesStorageMapName = processesStorageMapName;

        this.dbCollection = mongoTemplate.getCollection(processesStorageMapName);

        dbCollection.setDBDecoderFactory(bSerializationService.getDecoderFactory(Process.class));

        dbCollection.createIndex(new BasicDBObject(START_TIME_INDEX_NAME, 1).append(STATE_INDEX_NAME, 2));
    }

    // todo: should return whole Process object instead of its UUID to minimize quantity of queries to database
    @Override
    public IncompleteProcessesCursor findProcesses(long timeBefore, final int batchSize) {

        // { "startTime" : { "$lte" : 1423988513856} , "$or" : [ {"state": 0}, {"state": null} ]}
        BasicDBObject query = new BasicDBObject();
        query.append(START_TIME_INDEX_NAME, new BasicDBObject("$lte", timeBefore));

        BasicDBList orStates = new BasicDBList();
        orStates.add(new BasicDBObject(STATE_INDEX_NAME, null));
        orStates.add(new BasicDBObject(STATE_INDEX_NAME, Process.START));

        query.append("$or", orStates);

        logger.debug("Mongo query is " + query);

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
                DBObjectCheat dbObject = (DBObjectCheat) dbCursor.next();
                Process process = (Process) dbObject.getObject();
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
