package ru.taskurotta.service.hz.storage;

import com.hazelcast.core.HazelcastInstance;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.hz.serialization.bson.DecisionBSerializer;
import ru.taskurotta.service.storage.TaskUID;
import ru.taskurotta.transport.model.Decision;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: stukushin
 * Date: 14.06.2015
 * Time: 14:43
 */

public class MongoTaskDao extends HzTaskDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoTaskDao.class);

    private DBCollection decisionDBCollection;

    private static final String RECOVERY_TIME_NAME = DecisionBSerializer.RECOVERY_TIME.toString();

    public MongoTaskDao(HazelcastInstance hzInstance, String id2TaskMapName, String id2TaskDecisionMapName, DB mongoDB,
                        BSerializationService bSerializationService, String decisionCollectionName) {
        super(hzInstance, id2TaskMapName, id2TaskDecisionMapName);

        this.decisionDBCollection = mongoDB.getCollection(decisionCollectionName);
        this.decisionDBCollection.setDBDecoderFactory(bSerializationService.getDecoderFactory(Decision.class));
        this.decisionDBCollection.createIndex(new BasicDBObject(RECOVERY_TIME_NAME, 1));
    }

    public ResultSetCursor<TaskUID> findIncompleteTasks(long lastRecoveryTime, int batchSize) {
        BasicDBObject query = new BasicDBObject(RECOVERY_TIME_NAME, new BasicDBObject("$lte", lastRecoveryTime));
        return new DecisionTaskKeyResultSetCursor(decisionDBCollection, query, batchSize);
    }

    private class DecisionTaskKeyResultSetCursor implements ResultSetCursor<TaskUID> {

        private DBCollection dbCollection;
        private BasicDBObject query;
        private int batchSize;

        private DBCursor dbCursor;

        public DecisionTaskKeyResultSetCursor(DBCollection dbCollection, BasicDBObject query, int batchSize) {
            this.dbCollection = dbCollection;
            this.query = query;
            this.batchSize = batchSize;
        }

        public void open() {
            dbCursor = dbCollection.find(query).batchSize(batchSize);
        }

        @Override
        public Collection<TaskUID> getNext() {
            if (dbCursor == null) {
                open();
            }

            Collection<TaskUID> result = new ArrayList<>();

            int i = 0;
            while (i++ < batchSize && dbCursor.hasNext()) {
                DBObjectCheat dbObject = (DBObjectCheat) dbCursor.next();
                Decision decision = (Decision) dbObject.getObject();
                result.add(new TaskUID(decision.getTaskId(), decision.getProcessId()));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Found [{}] incomplete tasks", result.size());
            }

            return result;
        }

        @Override
        public void close() throws IOException {
            dbCursor.close();
        }
    }
}
