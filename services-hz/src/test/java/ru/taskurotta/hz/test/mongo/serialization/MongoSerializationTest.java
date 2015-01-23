package ru.taskurotta.hz.test.mongo.serialization;

import com.mongodb.*;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hz.test.mongo.serialization.custom.CustomBasicBSONEncoder;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by greg on 23/01/15.
 */
public class MongoSerializationTest {


    public void init() {

    }

    @Test
    public void testWithoutCustom() throws Exception {

        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);

        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "test-mongo");
        mongoTemplate.setWriteConcern(writeConcern);

        DBCollection withoutCol = mongoTemplate.createCollection("without-col");

        for (int i = 0; i < 500000; i++) {
            TaskContainer taskContainer = createTaskContainer();
            DBObject dbObject = new BasicDBObject();
            mongoTemplate.getConverter().write(taskContainer, dbObject);
            withoutCol.save(dbObject);
        }
    }

    @Test
    public void testWithCustom() throws Exception {

        DBEncoderFactory customDbEncoderFactory = new DBEncoderFactory() {
            @Override
            public DBEncoder create() {
                return new CustomBasicBSONEncoder();
            }
        };

        DefaultDBEncoder.FACTORY = customDbEncoderFactory;

        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);

        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "test-mongo");
        mongoTemplate.setWriteConcern(writeConcern);

        DBCollection withoutCol = mongoTemplate.createCollection("without-col");

        for (int i = 0; i < 5; i++) {
            TaskContainer taskContainer = createTaskContainer();
            DBObject dbObject = new BasicDBObject();
            dbObject.put("task-container", taskContainer);
            withoutCol.save(dbObject);
        }
    }

    private TaskContainer createTaskContainer() {
        UUID taskId = UUID.randomUUID();
        String method = "method";
        String actorId = "actorId";
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

        ArgContainer[] args = new ArgContainer[containerList.size()];
        containerList.toArray(args);
        UUID processId = UUID.randomUUID();
        String[] failTypes = {"java.lang.RuntimeException"};
        return new TaskContainer(taskId, processId, method, actorId, type, startTime, errorAttempts, args, null, true, failTypes);
    }

}
