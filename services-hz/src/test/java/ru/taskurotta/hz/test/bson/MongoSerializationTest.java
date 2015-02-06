package ru.taskurotta.hz.test.bson;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.hazelcast.queue.delay.impl.StorageItemContainer;
import ru.taskurotta.hazelcast.queue.delay.impl.mongodb.StorageItemContainerBSerializer;
import ru.taskurotta.hz.test.serialization.SerializationTest;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.mongodb.driver.DBObjectСheat;
import ru.taskurotta.mongodb.driver.impl.BDecoderFactory;
import ru.taskurotta.mongodb.driver.impl.BEncoderFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.hz.TaskFatKey;
import ru.taskurotta.service.hz.serialization.bson.DecisionContainerSerializer;
import ru.taskurotta.service.hz.serialization.bson.GraphSerializer;
import ru.taskurotta.service.hz.serialization.bson.ProcessSerializer;
import ru.taskurotta.service.hz.serialization.bson.UUIDSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by greg on 23/01/15.
 */
@Ignore
public class MongoSerializationTest {

    private MongoTemplate getMongoTemplate() throws UnknownHostException {
        MongoTemplate mongoTemplate = getMongoTemplateForDb("test-mongo");
        return mongoTemplate;
    }

    private MongoTemplate getMongoTemplateForDb(String db) throws UnknownHostException {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);

        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, db);
        mongoTemplate.setWriteConcern(writeConcern);
        return mongoTemplate;
    }


    @Test
    public void testProcess() throws UnknownHostException {
        MongoTemplate mongoTemplate = getMongoTemplate();
        DBCollection withCol = mongoTemplate.getCollection("process");

        ProcessSerializer processSerializer = new ProcessSerializer();

        withCol.setDBEncoderFactory(new BEncoderFactory(processSerializer));
        withCol.setDBDecoderFactory(new BDecoderFactory(processSerializer));

        for (int i = 0; i < 5; i++) {
            Process process = new Process(createTaskContainer());
            DBObjectСheat<Process> dbObject = new DBObjectСheat<>(process);
            withCol.insert(dbObject);
        }

        try (DBCursor cursor = withCol.find()) {
            while (cursor.hasNext()) {
                DBObjectСheat<Process> obj = (DBObjectСheat) cursor.next();
                System.out.println("actorId = " + obj.getObject().getStartTask().getActorId());
            }
        }
    }

    @Test
    public void testStorageItemSerializer() throws Exception {
        MongoTemplate mongoTemplate = getMongoTemplate();

        DBCollection withCol = mongoTemplate.getCollection("storage");

        UUIDSerializer uuidSerializer = new UUIDSerializer();
        StorageItemContainerBSerializer storageItemContainerBSerializer = new StorageItemContainerBSerializer(uuidSerializer);

        withCol.setDBEncoderFactory(new BEncoderFactory(storageItemContainerBSerializer));
        withCol.setDBDecoderFactory(new BDecoderFactory(storageItemContainerBSerializer));

        UUID uuid = UUID.randomUUID();
        StorageItemContainer storageItemContainer = new StorageItemContainer(uuid, 755757, "queName1");
        DBObjectСheat<StorageItemContainer> dbObjectСheat = new DBObjectСheat<>(storageItemContainer);
        withCol.save(dbObjectСheat);
    }


    @Test
    public void testGraph() throws Exception {

        MongoTemplate mongoTemplate = getMongoTemplate();

        DBCollection withCol = mongoTemplate.getCollection("graph");

        GraphSerializer graphSerializer = new GraphSerializer();

        withCol.setDBEncoderFactory(new BEncoderFactory(graphSerializer));
        withCol.setDBDecoderFactory(new BDecoderFactory(graphSerializer));

        for (int i = 0; i < 5; i++) {
            Graph graph = SerializationTest.newRandomGraph();
            DBObjectСheat<Graph> dbObject = new DBObjectСheat<>(graph);
            withCol.insert(dbObject);
        }

        try (DBCursor cursor = withCol.find()) {
            while (cursor.hasNext()) {
                DBObjectСheat<Graph> obj = (DBObjectСheat) cursor.next();
                System.out.println("finished = " + obj.getObject().getFinishedItems());
            }
        }
    }

//    @Test
//    public void testFindDecision() throws Exception{
//        MongoTemplate mongoTemplate = getMongoTemplateForDb("taskurotta");
//        DecisionContainerSerializer decisionContainerSerializer = new DecisionContainerSerializer();
//        final DBCollection withCol = mongoTemplate.getCollection("TaskDecision");
//        withCol.setDBEncoderFactory(new BEncoderFactory(decisionContainerSerializer));
//        withCol.setDBDecoderFactory(new BDecoderFactory(decisionContainerSerializer));
//        DBObject dbo = new BasicDBObject();
//        dbo.put("_id", new TaskFatKey(UUID.fromString("ea6ff60a-ae5b-4fab-ab62-0eb2c2e8c8a2"), UUID.fromString("880db255-f3f4-4ad6-b366-547254ec4730")));
//        DBObject founded = withCol.findOne(dbo);
//        System.out.println("founded = " + founded);
//    }


    @Test
    public void testDecisionContainer() throws UnknownHostException, InterruptedException {
        MongoTemplate mongoTemplate = getMongoTemplate();
        DecisionContainerSerializer decisionContainerSerializer = new DecisionContainerSerializer();
        mongoTemplate.dropCollection("decisions");
        final DBCollection withCol = mongoTemplate.getCollection("decisions");
        withCol.setDBEncoderFactory(new BEncoderFactory(decisionContainerSerializer));
        withCol.setDBDecoderFactory(new BDecoderFactory(decisionContainerSerializer));
        final ConcurrentLinkedQueue<TaskFatKey> queue = new ConcurrentLinkedQueue<>();
        final CountDownLatch lock = new CountDownLatch(1);
        final CountDownLatch lockToExit = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("STORE_THREAD");
                for (int i = 0; i < 10000; i++) {
                    DecisionContainer decisionContainer = createDecisionContainer();
                    TaskFatKey taskFatKey = new TaskFatKey(decisionContainer.getProcessId(), decisionContainer.getTaskId());
                    queue.add(taskFatKey);
                    DBObjectСheat<DecisionContainer> obj = new DBObjectСheat<>(decisionContainer);
                    withCol.insert(obj);
                    if (i == 1000) {
                        lock.countDown();
                    }
                }
            }
        });

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setName("READ_THREAD");
                    lock.await();
                    System.out.println("Checking...");
                    while (!queue.isEmpty()) {
                        DBObject dbo = new BasicDBObject();
                        dbo.put("_id", queue.poll());
                        DBObject founded = withCol.findOne(dbo);
                        Assert.assertNotNull(founded);
                    }
                    lockToExit.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        lockToExit.await();
    }

    @Test
    public void testDecisionContainerSearch() throws UnknownHostException {

        MongoTemplate mongoTemplate = getMongoTemplateForDb("taskurotta");
        DecisionContainerSerializer decisionContainerSerializer = new DecisionContainerSerializer();
        DBCollection withCol = mongoTemplate.getCollection("TaskDecision");
        withCol.setDBEncoderFactory(new BEncoderFactory(decisionContainerSerializer));
        withCol.setDBDecoderFactory(new BDecoderFactory(decisionContainerSerializer));


        TaskFatKey taskFatKey = new TaskFatKey(UUID.fromString("b4213c3a-890d-4b5f-b457-9eeb952a8f49"), UUID.fromString("78f96969-8d75-46d6-afd5-62784b28559d"));
        DBObject dbo = new BasicDBObject();
        dbo.put("_id", taskFatKey);
        DBObject founded = withCol.findOne(dbo);
        Assert.assertNotNull(founded);
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
        return new TaskContainer(
                taskId,
                processId,
                method,
                actorId,
                type,
                startTime,
                errorAttempts,
                args,
                taskOptionsContainer,
                true,
                failTypes);
    }

}
