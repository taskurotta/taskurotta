package ru.taskurotta.hz.test;

import com.hazelcast.util.Base64;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.BSerializationServiceFactory;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.hz.dependency.DecisionRow;
import ru.taskurotta.service.hz.serialization.bson.DecisionBSerializer;
import ru.taskurotta.service.hz.serialization.bson.DecisionRowBSerializer;
import ru.taskurotta.service.hz.serialization.bson.GraphBSerializer;
import ru.taskurotta.service.hz.serialization.bson.ProcessBSerializer;
import ru.taskurotta.service.hz.serialization.bson.TaskContainerBSerializer;
import ru.taskurotta.service.hz.serialization.bson.TaskKeyBSerializer;
import ru.taskurotta.service.hz.serialization.bson.UUIDBSerializer;
import ru.taskurotta.service.storage.GeneralTaskService;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.Decision;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
    static MongoMapStore graphDecisionStore;
    static MongoMapStore taskStore;
    static MongoMapStore taskDecisionStore;

    static TaskService taskService;

    public static void main(String[] args) throws Throwable {
        init();

        System.err.println("covert UUID to mongo base64: " +
                convertUUIDToMongoStyle(UUID.fromString("92f5daa8-57f9-4c08-b230-44593f61b00e")));
        System.err.println("covert mongo base64 to UUID: " +
                getUUID("g0YD7ek9wGsjbGciFJL8sA=="));


//        UUID processId = getUUID("I0ieD4Yc1LEHzv3XSCZAnw==");
        UUID processId = UUID.fromString("febd8d5a-1f35-43ec-9770-142e7f48a425");

        System.err.println("Date = " + new Date(2863196575625l));
        System.err.println("Process ID = " + processId + "\n");

        Process process = (Process) processStore.load(processId);

        System.err.println("Process = " + process + "\n");

        Graph graph = (Graph) graphStore.load(processId);

        System.err.println("Graph = " + graph + "\n");

        Map<UUID, Long> readyItems = graph.getAllReadyItems();

        if (readyItems == null) {
            System.err.println("Graph has no ready tasks");
        } else {
            for (UUID taskId : readyItems.keySet()) {
                TaskKey taskKey = new TaskKey(taskId, processId);
                System.err.println("ready Task " + taskId + ": ");
                TaskContainer taskContainer = (TaskContainer) taskStore.load(taskKey);
                System.err.println("Task = " + taskContainer);
                System.err.println("Task decision = " + taskDecisionStore.load(taskKey));

                try {
                System.err.println("Task container = " + taskService.getTaskToExecute(taskId, processId, true));
                } catch (IllegalStateException ex) {
                    System.err.println("Task container not ready yet..." + ex.getMessage());
                }

                System.err.println("Its arguments: ");
                for (ArgContainer taskArg : taskContainer.getArgs()) {
                    if (taskArg.isPromise() && !taskArg.isReady()) {
                        printAllTaskDecisions(taskArg, processId);
                    }

                    if (taskArg.getCompositeValue() != null) {
                        for (ArgContainer taskArgC : taskArg.getCompositeValue()) {
                            if (taskArgC.isPromise() && !taskArgC.isReady()) {

                                printAllTaskDecisions(taskArgC, processId);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void printAllTaskDecisions(ArgContainer taskArg, UUID processId) {
        Decision argTaskDecision = (Decision) taskDecisionStore.load(new TaskKey
                (taskArg.getTaskId(), processId));

        UUID argTaskId = taskArg.getTaskId();

        int i = 0;
        while (true) {

            TaskKey taskKey = new TaskKey(argTaskId, processId);

            System.err.println("" + i++ + " task (" + argTaskId + ") :" + argTaskDecision);
            System.err.println("Its decision row: " + graphDecisionStore.load(taskKey));

            if (argTaskDecision == null) {
                TaskContainer taskContainer = (TaskContainer) taskStore.load(taskKey);

                System.err.println("Task without decision is: " + taskContainer);
                break;
            }

            DecisionContainer decisionContainer = argTaskDecision.getDecisionContainer();

            if (decisionContainer == null) {
                System.err.println("decision container is null");
            }

            ArgContainer innerTaskArg = decisionContainer.getValue();

            if (innerTaskArg == null || !(innerTaskArg.isPromise() && !innerTaskArg
                    .isReady())) {
                break;
            }

            argTaskId = innerTaskArg.getTaskId();
            argTaskDecision = (Decision) taskDecisionStore.load(new
                    TaskKey(argTaskId, processId));
        }
    }


    private static UUID getUUID(String s) {
        byte[] bytes = Base64.decode(s.getBytes(Charset.forName("UTF-8")));

        long msb = org.bson.io.Bits.readLong(bytes, 0);
        long lsb = org.bson.io.Bits.readLong(bytes, 8);

        return new UUID(msb, lsb);
    }

    private static String convertUUIDToMongoStyle(UUID uuid) {

        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        byte[] bytes = new byte[16];
        writeLong(bytes, msb, 0);
        writeLong(bytes, lsb, 8);

        return new String(Base64.encode(bytes), Charset.forName("UTF-8"));
    }

    private static void writeLong(byte[] bytes, long x, int offset) {
        bytes[offset++] = (byte) (0xFFL & (x >> 0));
        bytes[offset++] = (byte) (0xFFL & (x >> 8));
        bytes[offset++] = (byte) (0xFFL & (x >> 16));
        bytes[offset++] = (byte) (0xFFL & (x >> 24));
        bytes[offset++] = (byte) (0xFFL & (x >> 32));
        bytes[offset++] = (byte) (0xFFL & (x >> 40));
        bytes[offset++] = (byte) (0xFFL & (x >> 48));
        bytes[offset++] = (byte) (0xFFL & (x >> 56));

    }

    private static void init() throws Throwable {
        MongoClient mongoClient = getMongoClient();
        DB mongoDB = mongoClient.getDB(DB_NAME);

        BSerializationService bSerializationService = BSerializationServiceFactory.newInstance(new UUIDBSerializer(),
                new ProcessBSerializer(), new GraphBSerializer(), new TaskContainerBSerializer(), new
                        DecisionBSerializer(), new TaskKeyBSerializer(), new DecisionRowBSerializer());

        processStore = new MongoMapStore(mongoDB, bSerializationService, Process.class.getName());
        processStore.init(null, new Properties(), COLLECTION_PROCESS);

        graphStore = new MongoMapStore(mongoDB, bSerializationService, Graph.class.getName());
        graphStore.init(null, new Properties(), COLLECTION_GRAPH);

        graphDecisionStore = new MongoMapStore(mongoDB, bSerializationService, DecisionRow.class.getName());
        graphDecisionStore.init(null, new Properties(), COLLECTION_GRAPH_DECISION);

        taskStore = new MongoMapStore(mongoDB, bSerializationService, TaskContainer.class.getName());
        taskStore.init(null, new Properties(), COLLECTION_TASK);

        taskDecisionStore = new MongoMapStore(mongoDB, bSerializationService, Decision.class.getName());
        taskDecisionStore.init(null, new Properties(), COLLECTION_TASK_DECISION);

        taskService = createTaskService(taskStore);

    }

    private static MongoClient getMongoClient() throws Throwable {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);
        mongoClient.setWriteConcern(new WriteConcern(1, 0, false, true));

        return mongoClient;
    }


    private static TaskService createTaskService(final MongoMapStore taskStore) {

        return new GeneralTaskService(new TaskDao() {
            @Override
            public UUID startTask(UUID taskId, UUID processId, long workerTimeout, boolean failOnWorkerTimeout) {
                return null;
            }

            @Override
            public boolean restartTask(UUID taskId, UUID processId, long timeToStart, boolean force) {
                return true;
            }

            @Override
            public boolean retryTask(UUID taskId, UUID processId, long timeToStart) {
                return true;
            }

            @Override
            public boolean finishTask(DecisionContainer taskDecision) {
                return true;
            }

            @Override
            public TaskContainer getTask(UUID taskId, UUID processId) {
                TaskKey taskKey = new TaskKey(taskId, processId);
                System.err.println("ready Task " + taskId + ": ");
                return (TaskContainer) taskStore.load(taskKey);
            }

            @Override
            public void addTask(TaskContainer taskContainer) {

            }

            @Override
            public void updateTask(TaskContainer taskContainer) {

            }

            @Override
            public DecisionContainer getDecision(UUID taskId, UUID processId) {

                TaskKey taskKey = new TaskKey(taskId, processId);
                return ((Decision) taskDecisionStore.load(taskKey)).getDecisionContainer();
            }

            @Override
            public boolean isTaskReleased(UUID taskId, UUID processId) {
                TaskKey taskKey = new TaskKey(taskId, processId);
                Decision decision = (Decision) taskDecisionStore.load(taskKey);

                if (decision == null) {
                    return false;
                }

                return decision.getState() == Decision.STATE_FINISH;
            }

            @Override
            public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize) {
                return null;
            }

            @Override
            public List<TaskContainer> getRepeatedTasks(int iterationCount) {
                return null;
            }

            @Override
            public void deleteTasks(Set<UUID> taskIds, UUID processId) {

            }

            @Override
            public void deleteDecisions(Set<UUID> decisionsIds, UUID processId) {

            }

            @Override
            public void archiveProcessData(UUID processId, Collection<UUID> finishedTaskIds) {

            }

            @Override
            public List<TaskContainer> findTasks(TaskSearchCommand command) {
                return null;
            }

            @Override
            public void updateTaskDecision(DecisionContainer taskDecision) {

            }
        }, 10000);
    }
}
