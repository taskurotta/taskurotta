package ru.taskurotta.hz.test;

import com.hazelcast.nio.BufferObjectDataInput;
import com.hazelcast.nio.BufferObjectDataOutput;
import com.hazelcast.nio.serialization.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.serialization.SerializationService;
import com.mongodb.DBCollection;
import org.bson.BSONObject;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.OutputBuffer;
import org.junit.Ignore;
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

import java.util.Arrays;

@Ignore
public class SerializationCompareTest {

    private final static Logger log = LoggerFactory.getLogger(SerializationCompareTest.class);
    public static final int COUNT = 100000;

    private static int RUN_TESTS = Integer.parseInt("1111", 2);

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

        TestBEncoder testBEncoder = new TestBEncoder(bSerializationService);
        BDecoder decoder = new BDecoder(serializer);

        DBObjectCheat<TaskContainer> dbObjectCheat = new DBObjectCheat<>(taskContainer);

        byteArray = null;


        // bson serialization
        if ((RUN_TESTS & (1 << 2)) != 0) {

            start = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();
                testBEncoder.serialize(basicOutputBuffer, dbObjectCheat);
                if (byteArray == null) {
                    byteArray = basicOutputBuffer.toByteArray();
                }
                testBEncoder.done();
            }

            log.info("BSON: Serialization took {} ms, object size {} bytes", System.currentTimeMillis() - start, byteArray
                    .length);
        }


        // bson deserialization
        if ((RUN_TESTS & (1 << 3)) != 0) {

            BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();
            testBEncoder.serialize(basicOutputBuffer, dbObjectCheat);
            byteArray = basicOutputBuffer.toByteArray();
            byteArray = Arrays.copyOfRange(byteArray, 4, byteArray.length);

            start = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                decoder.decode(byteArray, (DBCollection) null);
            }

            log.info("BSON: Deserialization took {} ms object size {} bytes", System.currentTimeMillis() - start,
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
}
