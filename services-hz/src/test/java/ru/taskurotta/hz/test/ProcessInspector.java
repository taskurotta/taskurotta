package ru.taskurotta.hz.test;

import com.hazelcast.util.Base64;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.BSerializationServiceFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.hz.serialization.bson.DecisionContainerBSerializer;
import ru.taskurotta.service.hz.serialization.bson.GraphBSerializer;
import ru.taskurotta.service.hz.serialization.bson.ProcessBSerializer;
import ru.taskurotta.service.hz.serialization.bson.TaskContainerBSerializer;
import ru.taskurotta.service.hz.serialization.bson.TaskKeyBSerializer;
import ru.taskurotta.service.hz.serialization.bson.UUIDBSerializer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 */
public class ProcessInspector {

    public static final String DB_NAME = "taskurotta";
    public static final String COLLECTION_PROCESS = "Process";
    public static final String COLLECTION_GRAPH = "Graph";
    public static final String COLLECTION_GRAPH_DECISION = "GraphDecision";
    public static final String COLLECTION_TASK = "Task";
    public static final String COLLECTION_TASK_DECISION = "TaskDecision";

    static MongoMapStore processStore;
    static MongoMapStore graphStore;
    static MongoMapStore taskStore;
    static MongoMapStore taskDecisionStore;

    public static void main(String[] args) throws Throwable {
        init();

        UUID processId = getUUID("Ok4hqsQftgiDyQCP00yJmw==");

        System.err.println("Process ID = " + processId + "\n");

        Process process = (Process) processStore.load(processId);

        System.err.println("Process = " + process + "\n");

        Graph graph = (Graph) graphStore.load(processId);

        System.err.println("Graph = " + graph + "\n");

        Map<UUID, Long> readyItems = graph.getAllReadyItems();

        if (readyItems == null) {
            System.err.println("Graph haz no ready tasks");
        } else {
            for (UUID taskId : readyItems.keySet()) {
                TaskKey taskKey = new TaskKey(taskId, processId);
                System.err.println("ready Task " + taskId + ": ");
                System.err.println("Task = " + taskStore.load(taskKey));
                System.err.println("Its decision = " + taskDecisionStore.load(taskKey));
            }
        }
    }


    private static UUID getUUID(String s) {
        byte[] bytes = Base64.decode(s.getBytes(Charset.forName("UTF-8")));

        long msb = org.bson.io.Bits.readLong(bytes, 0);
        long lsb = org.bson.io.Bits.readLong(bytes, 8);

        return new UUID(msb, lsb);
    }

    private static void init() throws Throwable {
        MongoClient mongoClient = getMongoClient();
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, DB_NAME);

        BSerializationService bSerializationService = BSerializationServiceFactory.newInstance(new UUIDBSerializer(),
                new ProcessBSerializer(), new GraphBSerializer(), new TaskContainerBSerializer(), new
                        DecisionContainerBSerializer(), new TaskKeyBSerializer());

        processStore = new MongoMapStore(mongoTemplate, bSerializationService, Process.class.getName());
        processStore.init(null, new Properties(), COLLECTION_PROCESS);

        graphStore = new MongoMapStore(mongoTemplate, bSerializationService, Graph.class.getName());
        graphStore.init(null, new Properties(), COLLECTION_GRAPH);

        taskStore = new MongoMapStore(mongoTemplate, bSerializationService, TaskContainer.class.getName());
        taskStore.init(null, new Properties(), COLLECTION_TASK);

        taskDecisionStore = new MongoMapStore(mongoTemplate, bSerializationService, DecisionContainer.class.getName());
        taskDecisionStore.init(null, new Properties(), COLLECTION_TASK_DECISION);

    }

    private static MongoClient getMongoClient() throws Throwable {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);
        mongoClient.setWriteConcern(new WriteConcern(1, 0, false, true));

        return mongoClient;
    }
}
