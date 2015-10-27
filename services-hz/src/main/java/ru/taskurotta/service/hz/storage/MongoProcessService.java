package ru.taskurotta.service.hz.storage;

import com.hazelcast.core.HazelcastInstance;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.hz.serialization.bson.ProcessBSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 */
public class MongoProcessService extends HzProcessService {

    private static final Logger logger = LoggerFactory.getLogger(MongoProcessService.class);

    private static final String START_TIME_FIELD_NAME = ProcessBSerializer.START_TIME.toString();
    private static final String END_TIME_FIELD_NAME = ProcessBSerializer.END_TIME.toString();
    private static final String STATE_FIELD_NAME = ProcessBSerializer.STATE.toString();

    private final DBCollection dbCollection;

    public MongoProcessService(HazelcastInstance hzInstance, String processesStorageMapName, DB mongoDB,
                               BSerializationService bSerializationService) {
        super(hzInstance, processesStorageMapName);

        this.dbCollection = mongoDB.getCollection(processesStorageMapName);
        this.dbCollection.setDBDecoderFactory(bSerializationService.getDecoderFactory(Process.class));

        // index for aborted processes
        this.dbCollection.createIndex(new BasicDBObject(START_TIME_FIELD_NAME, 1).append(STATE_FIELD_NAME, 1));
        // index for finished processes
        this.dbCollection.createIndex(new BasicDBObject(END_TIME_FIELD_NAME, 1).append(STATE_FIELD_NAME, 1));
    }

    @Override
    public ResultSetCursor findIncompleteProcesses(long recoveryTime, final int batchSize) {

        // { "startTime" : { "$lte" : 1423988513856} , "$or" : [ {"state": 0}, {"state": null} ]}
        BasicDBObject query = new BasicDBObject();
        query.append(START_TIME_FIELD_NAME, new BasicDBObject("$lte", recoveryTime));

        BasicDBList orStates = new BasicDBList();
        orStates.add(new BasicDBObject(STATE_FIELD_NAME, null));
        orStates.add(new BasicDBObject(STATE_FIELD_NAME, Process.START));

        query.append("$or", orStates);

        logger.debug("Mongo query is " + query);

        return new MongoResultSetCursor(dbCollection, query, batchSize);
    }

    @Override
    public ResultSetCursor<UUID> findLostProcesses(long lastFinishedProcessDeleteTime, long lastAbortedProcessDeleteTime, int batchSize) {
        BasicDBObject queryFinishedProcess =
                new BasicDBObject(END_TIME_FIELD_NAME, new BasicDBObject("$lte", lastFinishedProcessDeleteTime));
        queryFinishedProcess.append(STATE_FIELD_NAME, Process.FINISH);

        BasicDBObject queryAbortedProcess =
                new BasicDBObject(START_TIME_FIELD_NAME, new BasicDBObject("$lte", lastAbortedProcessDeleteTime));
        queryAbortedProcess.append(STATE_FIELD_NAME, Process.ABORTED);

        BasicDBList orStates = new BasicDBList();
        orStates.add(queryAbortedProcess);
        orStates.add(queryFinishedProcess);

        BasicDBObject query = new BasicDBObject();
        query.append("$or", orStates);

        logger.debug("Mongo query is " + query);

        return new MongoResultSetCursor(dbCollection, query, batchSize);
    }

    private class MongoResultSetCursor implements ResultSetCursor<UUID> {

        DBCollection dbCollection;
        BasicDBObject query;
        int batchSize;

        DBCursor dbCursor;

        public MongoResultSetCursor(DBCollection dbCollection, BasicDBObject query, int batchSize) {
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
            while (i++ < batchSize && dbCursor.hasNext()) {
                DBObjectCheat dbObject = (DBObjectCheat) dbCursor.next();
                Process process = (Process) dbObject.getObject();
                UUID processId = process.getProcessId();
                result.add(processId);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Found [{}] processes by query [{}]", result.size(), query);
            }

            return result;
        }
    }

}
