package ru.taskurotta.hz.test.bson;

import com.mongodb.DBCollection;
import org.bson.io.BasicOutputBuffer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.hz.test.serialization.SerializationTest;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.mongodb.driver.BSerializationServiceFactory;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.mongodb.driver.impl.BDecoder;
import ru.taskurotta.mongodb.driver.impl.BEncoder;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.hz.serialization.bson.*;
import ru.taskurotta.transport.model.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;


public class BsonSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(BsonSerializationTest.class);

    ObjectFactory factory = new ObjectFactory();

    @Test
    public void testArgContainer() {
        ArgContainer argContainer = factory.dumpArg(Promise.asPromise(2d*2d));
        int bytesLength = testBSerializer(new ArgContainerBSerializer(), argContainer);
        logger.debug("ArgContainer size is {}", bytesLength);
    }

    @Test
    public void testPromiseWithFailSerialization() throws Exception {
        ArgContainer argContainer = factory.dumpArg(Promise.asPromise(null));
        argContainer.setErrorContainer(factory.dumpError(new IllegalArgumentException("test exception")));
        int bytesLength = testBSerializer(new ArgContainerBSerializer(), argContainer);

        logger.debug("ArgContainer size is {}", bytesLength);
    }

    @Test
    public void testTaskContainer() throws Exception {

        TaskContainer taskContainer = createTaskContainer();
        TaskContainerBSerializer taskContainerBSerializer = new TaskContainerBSerializer();

        int bytesLen = testBSerializer(taskContainerBSerializer, taskContainer);

        logger.debug("TaskContainer size is {}", bytesLen);
    }

    @Test
    public void testProcess() throws UnknownHostException {

        Process process =  new Process(createTaskContainer());
        ProcessBSerializer processBSerializer = new ProcessBSerializer();

        int bytesLen = testBSerializer(processBSerializer, process);

        logger.debug("Process size is {}", bytesLen);
    }

//    @Test
//    public void testStorageItemSerializer() throws Exception {
//        MongoTemplate mongoTemplate = getMongoTemplate();
//
//        DBCollection withCol = mongoTemplate.getCollection("storage");
//
//        UUIDSerializer uuidSerializer = new UUIDSerializer();
//        StorageItemContainerBSerializer storageItemContainerBSerializer = new StorageItemContainerBSerializer(uuidSerializer);
//
//        withCol.setDBEncoderFactory(new BEncoderFactory(storageItemContainerBSerializer));
//        withCol.setDBDecoderFactory(new BDecoderFactory(storageItemContainerBSerializer));
//
//        UUID uuid = UUID.randomUUID();
//        StorageItemContainer storageItemContainer = new StorageItemContainer(uuid, 755757, "queName1");
//        DBObjectCheat<StorageItemContainer> dbObjectCheat = new DBObjectCheat<>(storageItemContainer);
//        withCol.save(dbObjectCheat);
//    }
//
//
    @Test
    public void testGraph() throws Exception {

        Graph graph = SerializationTest.newRandomGraph();
        GraphBSerializer graphBSerializer = new GraphBSerializer();

        int bytesLen = testBSerializer(graphBSerializer, graph);

        logger.debug("Graph size is {}", bytesLen);
    }

    @Test
    public void testDecisionContainer() throws UnknownHostException {

        DecisionContainer decisionContainer = createDecisionContainer();
        DecisionContainerBSerializer decisionContainerBSerializer = new DecisionContainerBSerializer();

        int bytesLen = testBSerializer(decisionContainerBSerializer, decisionContainer);

        logger.debug("DecisionContainer size is {}", bytesLen);

    }


    // encode and decode object
    public int testBSerializer(StreamBSerializer serializer, Object obj) {

        BEncoder bEncoder = new BEncoder(BSerializationServiceFactory.newInstance(serializer));
        BDecoder bDecoder = new BDecoder(serializer);

        DBObjectCheat dbObjectCheat = new DBObjectCheat(obj);

        BasicOutputBuffer bob = new BasicOutputBuffer();

        bEncoder.writeObject(bob, dbObjectCheat);

        byte[] bytes = bob.toByteArray();
        // crop array length
        bytes = Arrays.copyOfRange(bytes, 4, bytes.length);

        DBObjectCheat dbObject = (DBObjectCheat) bDecoder.decode(bytes, (DBCollection) null);
        assertEquals(obj, dbObject.getObject());

        return bytes.length;
    }

    public static DecisionContainer createDecisionContainer() {
        ArgContainer argContainer1 = new ArgContainer();
        argContainer1.setTaskId(UUID.randomUUID());
        argContainer1.setDataType("simple1");
        argContainer1.setJSONValue("jsonData1");
        argContainer1.setPromise(false);
        argContainer1.setReady(true);
        argContainer1.setValueType(ArgContainer.ValueType.COLLECTION);

        TaskContainer[] taskContainers = new TaskContainer[2];
        taskContainers[0] = createTaskContainer();
        taskContainers[1] = createTaskContainer();

        ErrorContainer errorContainer = new ErrorContainer();
        errorContainer.setClassNames(new String[]{"test", "test1"});
        errorContainer.setMessage("messageErr");
        errorContainer.setStackTrace("stack");

        return new DecisionContainer(UUID.randomUUID(), UUID.randomUUID(), argContainer1, errorContainer, 6666, taskContainers, "act4", 7777);
    }


    public static TaskContainer createTaskContainer() {
        UUID taskId = UUID.randomUUID();
        String method = "method";
        String actorId = "actorId#" + taskId.toString();
        TaskType type = TaskType.DECIDER_START;
        long startTime = 15121234;
        int errorAttempts = 2;

        List<ArgContainer> containerList = new ArrayList<>();
        ArgContainer argContainer1 = new ArgContainer();
        argContainer1.setTaskId(UUID.randomUUID());
        argContainer1.setDataType("simple1");
        argContainer1.setJSONValue("jsonData1");
        argContainer1.setPromise(false);
        argContainer1.setReady(true);
        argContainer1.setValueType(ArgContainer.ValueType.COLLECTION);

        ArgContainer argContainer2 = new ArgContainer();
        argContainer2.setTaskId(UUID.randomUUID());
        argContainer2.setDataType("simple1");
        argContainer2.setJSONValue("jsonData1");
        argContainer2.setPromise(false);
        argContainer2.setReady(true);
        argContainer2.setValueType(ArgContainer.ValueType.COLLECTION);

        argContainer2.setCompositeValue(new ArgContainer[]{argContainer1});

        containerList.add(argContainer1);
        containerList.add(argContainer2);

        TaskConfigContainer taskConfigContainer = new TaskConfigContainer();
        taskConfigContainer.setCustomId("customId#" + UUID.randomUUID());

        taskConfigContainer.setStartTime(89689798);
        taskConfigContainer.setTaskList("task list");
        RetryPolicyConfigContainer retryPolicyConfigContainer = new RetryPolicyConfigContainer();
        retryPolicyConfigContainer.setBackoffCoefficient(0.8);
        retryPolicyConfigContainer.setExceptionsToExclude(Arrays.asList("str", "rstrfg"));
        retryPolicyConfigContainer.setExceptionsToRetry(Arrays.asList("str56", "758687"));
        retryPolicyConfigContainer.setMaximumAttempts(5);
        retryPolicyConfigContainer.setInitialRetryIntervalSeconds(100);
        retryPolicyConfigContainer.setRetryExpirationIntervalSeconds(10);
        retryPolicyConfigContainer.setMaximumRetryIntervalSeconds(60);
        retryPolicyConfigContainer.setType(RetryPolicyConfig.RetryPolicyType.LINEAR);

        taskConfigContainer.setRetryPolicyConfigContainer(retryPolicyConfigContainer);

        ArgType[] argTypes = {ArgType.NO_WAIT, null, ArgType.NONE};

        ArgContainer[] args = new ArgContainer[containerList.size()];
        containerList.toArray(args);
        TaskOptionsContainer taskOptionsContainer = new TaskOptionsContainer(
                argTypes,
                taskConfigContainer,
                args);


        UUID processId = UUID.randomUUID();
        String[] failTypes = {"java.lang.RuntimeException"};
        return new TaskContainer(taskId, processId, method, actorId, type, startTime, errorAttempts, args,
                taskOptionsContainer, true, failTypes);
    }

}
