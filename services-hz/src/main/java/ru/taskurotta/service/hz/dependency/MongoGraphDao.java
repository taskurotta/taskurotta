package ru.taskurotta.service.hz.dependency;

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
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.hz.serialization.bson.GraphBSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 08.10.2015
 * Time: 13:37
 */

public class MongoGraphDao extends HzGraphDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoGraphDao.class);

    private DBCollection graphDBCollection;
    private String graphsMapName;
    private DB mongoDB;
    private BSerializationService bSerializationService;

    private static final String LAST_TOUCH_TIME_NAME = GraphBSerializer.LAST_TOUCH_TIME.toString();

    public MongoGraphDao(HazelcastInstance hzInstance, String graphsMapName, DB mongoDB,
                         BSerializationService bSerializationService) {
        super(hzInstance, graphsMapName);

        this.graphsMapName = graphsMapName;
        this.mongoDB = mongoDB;
        this.bSerializationService = bSerializationService;

        init();
    }

    public MongoGraphDao(HazelcastInstance hzInstance, DB mongoDB, BSerializationService bSerializationService) {
        super(hzInstance);

        this.mongoDB = mongoDB;
        this.bSerializationService = bSerializationService;

        init();
    }

    private void init() {
        this.graphDBCollection = mongoDB.getCollection(graphsMapName);
        this.graphDBCollection.setDBDecoderFactory(bSerializationService.getDecoderFactory(Graph.class));
        this.graphDBCollection.createIndex(new BasicDBObject(LAST_TOUCH_TIME_NAME, 1));
    }

    @Override
    public ResultSetCursor<UUID> findLostGraphs(long lastGraphChangeTime, int batchSize) {
        return new ResultSetCursor<UUID>() {

            private DBCursor dbCursor;

            private void createCursor() {
                BasicDBObject query = new BasicDBObject(LAST_TOUCH_TIME_NAME,
                        new BasicDBObject("$lte", lastGraphChangeTime));

                this.dbCursor = graphDBCollection.find(query).batchSize(batchSize);
            }

            @Override
            public Collection<UUID> getNext() {
                if (dbCursor == null) {
                    createCursor();
                }

                Collection<UUID> result = new ArrayList<>(batchSize);

                int i = 0;
                while (i++ < batchSize && dbCursor.hasNext()) {
                    DBObjectCheat dbObject = (DBObjectCheat) dbCursor.next();
                    Graph graph = (Graph) dbObject.getObject();
                    result.add(graph.getGraphId());
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Found [{}] lost graphs", result.size());
                }

                return result;
            }

            @Override
            public void close() throws IOException {
                dbCursor.close();
            }
        };
    }
}
