package ru.taskurotta.hz.test;

import com.hazelcast.nio.BufferObjectDataInput;
import com.hazelcast.nio.BufferObjectDataOutput;
import com.hazelcast.nio.serialization.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.serialization.SerializationService;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBDecoder;
import com.mongodb.DBEncoder;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBDecoder;
import com.mongodb.DefaultDBEncoder;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.bson.BSONObject;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.OutputBuffer;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import ru.taskurotta.hz.test.bson.BsonSerializationTest;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.mongodb.driver.impl.BDecoder;
import ru.taskurotta.mongodb.driver.impl.BEncoder;
import ru.taskurotta.mongodb.driver.impl.BSerializationServiceImpl;
import ru.taskurotta.service.hz.serialization.TaskContainerStreamSerializer;
import ru.taskurotta.service.hz.serialization.bson.TaskContainerBSerializer;
import ru.taskurotta.transport.model.TaskContainer;

import java.net.UnknownHostException;
import java.util.Arrays;

@Ignore
public class SerializationCompareTest {

    private final static Logger log = LoggerFactory.getLogger(SerializationCompareTest.class);
    public static final int COUNT = 100000;

    private static int RUN_TESTS = Integer.parseInt("111111", 2);

    @Test
    public void test() throws Exception {
        TaskContainerStreamSerializer streamSerializer = new TaskContainerStreamSerializer();
        DefaultSerializationServiceBuilder builder = new DefaultSerializationServiceBuilder();
        SerializationService serializationService = builder.build();
        final BufferObjectDataOutput objectDataOutput = serializationService.createObjectDataOutput(1024);

        TaskContainer taskContainer = BsonSerializationTest.createTaskContainer();
        long start;

        byte[] byteArray = null;


        //hz serialization
        if ((RUN_TESTS & (1 << 0)) != 0) {

            start = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                streamSerializer.write(objectDataOutput, taskContainer);
                if (byteArray == null) {
                    byteArray = objectDataOutput.toByteArray();
                }
                objectDataOutput.clear();

            }

            log.info("HZ: Serialization took {} ms, object size {} bytes", System.currentTimeMillis() - start, byteArray
                    .length);
        }


        //hz deserialization
        if ((RUN_TESTS & (1 << 1)) != 0) {

            streamSerializer.write(objectDataOutput, taskContainer);
            byteArray = objectDataOutput.toByteArray();

            start = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                BufferObjectDataInput objectDataInput = serializationService.createObjectDataInput(byteArray);
                streamSerializer.read(objectDataInput);
            }

            log.info("HZ: Deserialization took {} ms object size {} bytes", System.currentTimeMillis() - start, byteArray
                    .length);
        }


        final TaskContainerBSerializer serializer = new TaskContainerBSerializer();
        StreamBSerializer[] array = new StreamBSerializer[]{serializer};
        BSerializationService bSerializationService = new BSerializationServiceImpl(array, null);
        TestBEncoder encoder = new TestBEncoder(bSerializationService);

        DBObjectCheat<TaskContainer> dbObjectCheat = new DBObjectCheat<>(taskContainer);

        byteArray = null;


        // bson serialization
        if ((RUN_TESTS & (1 << 2)) != 0) {

            start = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();
                encoder.serialize(basicOutputBuffer, dbObjectCheat);
                if (byteArray == null) {
                    byteArray = basicOutputBuffer.toByteArray();
                }
                encoder.done();
            }

            log.info("BSON: Serialization took {} ms, object size {} bytes", System.currentTimeMillis() - start, byteArray
                    .length);
        }


        // bson deserialization
        if ((RUN_TESTS & (1 << 3)) != 0) {

            BDecoder decoder = new BDecoder(serializer);

            BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();
            encoder.serialize(basicOutputBuffer, dbObjectCheat);
            byteArray = basicOutputBuffer.toByteArray();
            byteArray = Arrays.copyOfRange(byteArray, 4, byteArray.length);

            start = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                decoder.decode(byteArray, (DBCollection) null);
            }

            log.info("BSON: Deserialization took {} ms object size {} bytes", System.currentTimeMillis() - start,
                    byteArray.length);
        }


        // DBObject serialization
        if ((RUN_TESTS & (1 << 4)) != 0) {

            MongoTemplate mongoTemplate = getMongoTemplate();
            MongoConverter converter = mongoTemplate.getConverter();

            DBEncoder dbEncoder = DefaultDBEncoder.FACTORY.create();

            start = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {

                DBObject dbObject = new BasicDBObject();
                converter.write(taskContainer, dbObject);

                BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();

                dbEncoder.writeObject(basicOutputBuffer, dbObject);

                if (byteArray == null) {
                    byteArray = basicOutputBuffer.toByteArray();
                }
            }

            log.info("DBObject: Serialization took {} ms, object size {} bytes", System.currentTimeMillis() - start, byteArray
                    .length);
        }


        // DBObject deserialization
        if ((RUN_TESTS & (1 << 5)) != 0) {

            MongoTemplate mongoTemplate = getMongoTemplate();
            MongoConverter converter = mongoTemplate.getConverter();
            DBEncoder dbEncoder = DefaultDBEncoder.FACTORY.create();
            DBObject dbObject = new BasicDBObject();
            converter.write(taskContainer, dbObject);
            BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();
            dbEncoder.writeObject(basicOutputBuffer, dbObject);
            byteArray = basicOutputBuffer.toByteArray();

            DBDecoder dbDecoder = DefaultDBDecoder.FACTORY.create();

            start = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                DBObject decodedDbObject = dbDecoder.decode(byteArray, (DBCollection) null);
                converter.read(TaskContainer.class, decodedDbObject);
            }

            log.info("DBObject: Deserialization took {} ms object size {} bytes", System.currentTimeMillis() - start,
                    byteArray.length);
        }

    }


    class TestBEncoder extends BEncoder {

        public TestBEncoder(BSerializationService bSerializationService) {
            super(bSerializationService);
        }

        public void serialize(OutputBuffer outputBuffer, BSONObject bsonObject) {
            set(outputBuffer);
            handleSpecialObjects(null, bsonObject);
        }

    }

    private MongoTemplate getMongoTemplate() throws UnknownHostException {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);

        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "test");
        mongoTemplate.setWriteConcern(writeConcern);

        return mongoTemplate;
    }

}
