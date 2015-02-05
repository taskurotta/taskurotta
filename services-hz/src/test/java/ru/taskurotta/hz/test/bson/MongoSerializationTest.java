package ru.taskurotta.hz.test.bson;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.hz.test.serialization.SerializationTest;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.mongodb.driver.DBObjectСheat;
import ru.taskurotta.mongodb.driver.impl.BDecoderFactory;
import ru.taskurotta.mongodb.driver.impl.BEncoderFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.hz.serialization.bson.GraphSerializer;
import ru.taskurotta.service.hz.serialization.bson.ProcessSerializer;
import ru.taskurotta.service.hz.serialization.bson.TaskContainerSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by greg on 23/01/15.
 */
@Ignore
public class MongoSerializationTest {

    private MongoTemplate getMongoTemplate() throws UnknownHostException {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);

        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "test-mongo");
        mongoTemplate.setWriteConcern(writeConcern);
        return mongoTemplate;
    }

    @Test
    public void testTaskContainer() throws Exception {

        MongoTemplate mongoTemplate = getMongoTemplate();

        DBCollection withCol = mongoTemplate.getCollection("taskContainers");

        TaskContainerSerializer taskContainerSerializer = new TaskContainerSerializer();

        withCol.setDBEncoderFactory(new BEncoderFactory(taskContainerSerializer));
        withCol.setDBDecoderFactory(new BDecoderFactory(taskContainerSerializer));

        for (int i = 0; i < 5; i++) {
            TaskContainer taskContainer = createTaskContainer();
            DBObjectСheat dbObject = new DBObjectСheat(taskContainer);
            withCol.insert(dbObject);
        }

        try (DBCursor cursor = withCol.find()) {
            while (cursor.hasNext()) {
                DBObjectСheat obj = (DBObjectСheat) cursor.next();
                System.out.println("actorId = " + ((TaskContainer) obj.getObject()).getActorId());
            }
        }
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


    private TaskContainer createTaskContainer() {
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

        ArgType[] argTypes = {ArgType.NO_WAIT, ArgType.NONE};

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
