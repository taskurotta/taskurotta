package ru.taskurotta.hz.test;

import com.hazelcast.nio.BufferObjectDataInput;
import com.hazelcast.nio.BufferObjectDataOutput;
import com.hazelcast.nio.serialization.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.serialization.SerializationService;
import com.mongodb.DBCollection;
import org.bson.BSONObject;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.OutputBuffer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.DataInputStream;

/**
 * Created by greg on 09/02/15.
 */
public class SerializationCompareTest {

    private final static Logger log = LoggerFactory.getLogger(SerializationCompareTest.class);
    public static final int COUNT = 100000;


    @Test
    public void test() throws Exception {
        TaskContainerStreamSerializer streamSerializer = new TaskContainerStreamSerializer();
        DefaultSerializationServiceBuilder builder = new DefaultSerializationServiceBuilder();
        SerializationService serializationService = builder.build();
        final BufferObjectDataOutput objectDataOutput = serializationService.createObjectDataOutput(1024);

        TaskContainer taskContainer = BsonSerializationTest.createTaskContainer();
        byte[] hzArray = null;
        //hz serialization
        int objectSize = 0;
        long start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            streamSerializer.write(objectDataOutput, taskContainer);
            if (hzArray == null) {
                hzArray = objectDataOutput.toByteArray();
                objectSize = hzArray.length;
            }
            objectDataOutput.clear();

        }
        long serialzationResult = System.currentTimeMillis() - start;
        //hz deserialization
        start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            BufferObjectDataInput objectDataInput = serializationService.createObjectDataInput(hzArray);
            TaskContainer taskContainerRestored = streamSerializer.read(objectDataInput);
        }
        long deserialzationResult = System.currentTimeMillis() - start;
        log.info("HZ: Serialization took {} ms | Deserialization took {} ms | Object size {}", serialzationResult, deserialzationResult, objectSize);


        final TaskContainerBSerializer serializer = new TaskContainerBSerializer();
        StreamBSerializer[] array = new StreamBSerializer[]{serializer};
        BSerializationService bSerializationService = new BSerializationServiceImpl(array, null);

        TestBEncoder testBEncoder = new TestBEncoder(bSerializationService);
        BDecoder decoder = new BDecoder(serializer);

        DBObjectCheat<TaskContainer> dbObjectCheat = new DBObjectCheat<>(taskContainer);

        // bson serialization
        int bsonObjectSize = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();
            testBEncoder.serialize(basicOutputBuffer, dbObjectCheat);
            if (bsonObjectSize == 0) {
                bsonObjectSize = basicOutputBuffer.toByteArray().length;
            }
            testBEncoder.done();
        }
        long result = System.currentTimeMillis() - start;

        DBCollection dbCollection = null;
        DataInputStream in = new DataInputStream(SerializationCompareTest.class.getResourceAsStream("/obj.ser"));
        byte[] bytes = new byte[1350];
        in.readFully(bytes);

        // bson deserialization
        start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            DBObjectCheat<TaskContainer> objectCheat = (DBObjectCheat) decoder.decode(bytes, dbCollection);
            TaskContainer container = objectCheat.getObject();
        }
        long deSerializeStop = System.currentTimeMillis() - start;
        log.info("BSON: Serialization took {} ms | Deserialization took {} ms | Object size {}", result, deserialzationResult, bsonObjectSize);
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
}
