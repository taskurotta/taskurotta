package ru.taskurotta.hz.test;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.hz.test.bson.BsonSerializationTest;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.BSerializationServiceFactory;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.hz.serialization.bson.DecisionContainerBSerializer;
import ru.taskurotta.service.hz.serialization.bson.TaskKeyBSerializer;
import ru.taskurotta.transport.model.DecisionContainer;

import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

@Ignore
public class MongoMapStoreTest {

    @Test
    public void test() throws UnknownHostException {


        BSerializationService serializationService = BSerializationServiceFactory.newInstance(new
                DecisionContainerBSerializer(), new
                TaskKeyBSerializer());

        MongoMapStore mongoMapStore = new MongoMapStore(getMongoDB(), serializationService, "ru.taskurotta" +
                ".transport.model.DecisionContainer");

        mongoMapStore.init(null, new Properties(), "Decisions");

        UUID taskId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        DecisionContainer decisionContainer = BsonSerializationTest.createDecisionContainer();

        decisionContainer.setTaskId(taskId);
        decisionContainer.setProcessId(processId);

        TaskKey taskKey = new TaskKey(UUID.randomUUID(), UUID.randomUUID());

        mongoMapStore.store(null, decisionContainer);
        assertNotNull(mongoMapStore.load(taskKey));

    }

    private DB getMongoDB() throws UnknownHostException {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);
        mongoClient.setWriteConcern(writeConcern);

        return mongoClient.getDB("test");
    }

}
