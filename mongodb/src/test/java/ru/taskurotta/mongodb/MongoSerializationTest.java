package ru.taskurotta.mongodb;

import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.mongodb.domain.RootPojo;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.BSerializationServiceFactory;
import ru.taskurotta.mongodb.driver.impl.DBObjectСheat;
import ru.taskurotta.mongodb.io.RootPojoStreamBSerializer;

import java.net.UnknownHostException;

@Ignore
public class MongoSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(MongoSerializationTest.class);

    private MongoTemplate getMongoTemplate() throws UnknownHostException {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);

        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "test");
        mongoTemplate.setWriteConcern(writeConcern);

        mongoTemplate.getDb().dropDatabase();
        return mongoTemplate;
    }

    @Test
    @Ignore
    public void testDefaultEncoder() throws Exception {

        final MongoTemplate mongoTemplate = getMongoTemplate();
        final DBCollection oldCollection = mongoTemplate.getCollection("old");

        final SpringMongoDBConverter converter = new SpringMongoDBConverter(mongoTemplate);


        final WriteConcern writeConcern = oldCollection.getWriteConcern();

        long startTime = System.currentTimeMillis();


        for (int i = 0; i < 100000; i++) {

            DBObject dbo = converter.toDBObject(new RootPojo(i));
            dbo.put("_id", i);

            oldCollection.insert(dbo);
        }

        // 25431
        logger.debug("Done: {} milliseconds", (System.currentTimeMillis() - startTime));

    }

    @Test
    @Ignore
    public void testCustomEncoder() throws Exception {

        final MongoTemplate mongoTemplate = getMongoTemplate();

        BSerializationService serializationService = BSerializationServiceFactory.newInstance();
        serializationService.registerSerializers(RootPojo.class, new RootPojoStreamBSerializer());


        final DBCollection newCollection = mongoTemplate.getCollection("new");
        newCollection.setDBEncoderFactory(serializationService);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {

            DBObjectСheat document = new DBObjectСheat(new RootPojo(i));

            newCollection.insert(document);
        }

        // 19605
        logger.debug("Done: {} milliseconds", (System.currentTimeMillis() - startTime));

    }

}
