package ru.taskurotta.mongodb.driver;

import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.mongodb.driver.domain.RootPojo;
import ru.taskurotta.mongodb.driver.impl.BDecoderFactory;
import ru.taskurotta.mongodb.driver.impl.BEncoderFactory;
import ru.taskurotta.mongodb.driver.io.RootPojoStreamBSerializer;

import java.net.UnknownHostException;

@Ignore
public class MongoSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(MongoSerializationTest.class);

    public static final int COLLECTION_SIZE = 100000;

    private MongoTemplate getMongoTemplate() throws UnknownHostException {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);

        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "test");
        mongoTemplate.setWriteConcern(writeConcern);

        return mongoTemplate;
    }

    @Test
    @Ignore
    public void testDefaultEncoder() throws Exception {

        final MongoTemplate mongoTemplate = getMongoTemplate();
        mongoTemplate.getDb().dropDatabase();

        final DBCollection coll = mongoTemplate.getCollection("old");

        final SpringMongoDBConverter converter = new SpringMongoDBConverter(mongoTemplate);


        final WriteConcern writeConcern = coll.getWriteConcern();

//        TimeUnit.SECONDS.sleep(60);

        long startTime = System.currentTimeMillis();

        logger.debug("Start to insert {} elements...", COLLECTION_SIZE);

        for (int i = 0; i < COLLECTION_SIZE; i++) {

            DBObject dbo = converter.toDBObject(new RootPojo(i));
            dbo.put("_id", i);

            coll.insert(dbo);
        }

        // 25431
        logger.debug("Done: {} milliseconds", (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();

        logger.debug("Start to load {} elements...", COLLECTION_SIZE);

        int i = 0;
        try (DBCursor cursor = coll.find()) {
            while (cursor.hasNext()) {
                try {
                    DBObject obj = cursor.next();
                    Class clazz = Class.forName(obj.get("_class").toString());
                    converter.toObject(clazz, obj);
                    i++;
                } catch (ClassNotFoundException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        // 4358
        logger.debug("Done read {} elements: {} milliseconds", i, (System.currentTimeMillis() - startTime));

        // memory up from 56 to 393 mb
//        TimeUnit.SECONDS.sleep(20);
    }

    @Test
    @Ignore
    public void testCustomEncoder() throws Exception {

        final MongoTemplate mongoTemplate = getMongoTemplate();
        mongoTemplate.getDb().dropDatabase();

        RootPojoStreamBSerializer rootPojoStreamBSerializer = new RootPojoStreamBSerializer();

        final DBCollection coll = mongoTemplate.getCollection("new");
        coll.setDBEncoderFactory(new BEncoderFactory(rootPojoStreamBSerializer));
        coll.setDBDecoderFactory(new BDecoderFactory(rootPojoStreamBSerializer));

//        TimeUnit.SECONDS.sleep(60);

        long startTime = System.currentTimeMillis();

        logger.debug("Start to insert {} elements...", COLLECTION_SIZE);

        for (int i = 0; i < COLLECTION_SIZE; i++) {

            DBObjectСheat document = new DBObjectСheat(new RootPojo(i));

            coll.insert(document);
        }

        // 18837
        logger.debug("Done: {} milliseconds", (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();

        logger.debug("Start to load {} elements...", COLLECTION_SIZE);

        int i = 0;
        try (DBCursor cursor = coll.find()) {
            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                if (obj instanceof DBObjectСheat && ((DBObjectСheat) obj).getObject() != null) {
                    i++;
                }
            }
        }

        // 1092
        logger.debug("Done read {} elements: {} milliseconds", i, (System.currentTimeMillis() - startTime));


        // find by query
        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", Integer.MIN_VALUE).append("$lte",
                Integer.MAX_VALUE));

        startTime = System.currentTimeMillis();

        logger.debug("Start to load {} elements with simple query...", COLLECTION_SIZE);

        int j = 0;
        try (DBCursor cursor = coll.find(query).batchSize(100)) {
            while (cursor.hasNext()) {

                DBObject obj = cursor.next();

                RootPojo rootPojo = (RootPojo) ((DBObjectСheat) obj).getObject();
                if (rootPojo != null) {
                    j++;
                }
            }
        }

        logger.debug("Done read {} elements: {} milliseconds", j, (System.currentTimeMillis() - startTime));


//        TimeUnit.SECONDS.sleep(20);

    }

}
