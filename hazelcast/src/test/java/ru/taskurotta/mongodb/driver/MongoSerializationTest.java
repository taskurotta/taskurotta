package ru.taskurotta.mongodb.driver;

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
import ru.taskurotta.mongodb.driver.io.RootPojoStreamBSerializer;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;

@Ignore
public class MongoSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(MongoSerializationTest.class);

    public static final int COLLECTION_SIZE = 100000;

    public static WriteConcern noWaitWriteConcern = new WriteConcern(0, 0, false, true);


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
    public void testCustomEncoder() throws Exception {

        String str = "hehehe";
        Date date = new Date();
        UUID uuid = UUID.randomUUID();
        String str2 = "hohoho";
        UUID uuid2 = UUID.randomUUID();


        final MongoTemplate mongoTemplate = getMongoTemplate();
        mongoTemplate.getDb().dropDatabase();

        RootPojoStreamBSerializer rootPojoStreamBSerializer = new RootPojoStreamBSerializer();

        final DBCollection coll = mongoTemplate.getCollection("new");
        coll.setDBEncoderFactory(new BEncoderFactory(BSerializationServiceFactory.newInstance
                (rootPojoStreamBSerializer)));
        coll.setDBDecoderFactory(new BDecoderFactory(rootPojoStreamBSerializer));

//        TimeUnit.SECONDS.sleep(60);

        long startTime = System.currentTimeMillis();

        logger.debug("Start to insert {} elements...", COLLECTION_SIZE);

        for (int i = 0; i < COLLECTION_SIZE; i++) {

            DBObjectCheat<RootPojo> document = new DBObjectCheat<>(new RootPojo(i, str, date, uuid, str2, uuid2));

            coll.insert(document);
        }

        // 15132
        logger.debug("Done: {} milliseconds", (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();

        RootPojo rootPojo = null; //new RootPojo(1, str, date, uuid, str2, uuid2);

        logger.debug("Start to load {} elements...", COLLECTION_SIZE);

        int i = 0;
        try (DBCursor cursor = coll.find()) {
            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                if (obj instanceof DBObjectCheat && ((DBObjectCheat) obj).getObject() != null) {
                    i++;

                    if (rootPojo != null) {
                        assertEquals(rootPojo, ((DBObjectCheat) obj).getObject());
                    }
                }
            }
        }

        // 577
        logger.debug("Done read {} elements: {} milliseconds", i, (System.currentTimeMillis() - startTime));


        startTime = System.currentTimeMillis();

        logger.debug("Start to remove {} elements ...", COLLECTION_SIZE);

        int j = 0;

        DBObjectID objectID = new DBObjectID(null);

        for (j = 0; j < COLLECTION_SIZE; j++) {

            objectID.setId(j);
            coll.remove(objectID, noWaitWriteConcern);
        }


        // 493
        logger.debug("Done remove {} elements: {} milliseconds", j, (System.currentTimeMillis() - startTime));


//        TimeUnit.SECONDS.sleep(20);

    }

}
