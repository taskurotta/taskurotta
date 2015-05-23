package ru.taskurotta.hz.test.serialization;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.hazelcast.util.ConfigUtil;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.config.model.ActorPreferences;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.Modification;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.hz.dependency.DecisionRow;
import ru.taskurotta.service.hz.serialization.ActorPreferencesStreamSerializer;
import ru.taskurotta.service.hz.serialization.ArgContainerStreamSerializer;
import ru.taskurotta.service.hz.serialization.DecisionRowStreamSerializer;
import ru.taskurotta.service.hz.serialization.ErrorContainerStreamSerializer;
import ru.taskurotta.service.hz.serialization.GraphStreamSerializer;
import ru.taskurotta.service.hz.serialization.ProcessDecisionUnitOfWorkStreamSerializer;
import ru.taskurotta.service.hz.serialization.RecoveryOperationStreamSerializer;
import ru.taskurotta.service.hz.serialization.TaskConfigContainerStreamSerializer;
import ru.taskurotta.service.hz.serialization.TaskContainerStreamSerializer;
import ru.taskurotta.service.hz.serialization.TaskOptionsContainerSerializer;
import ru.taskurotta.service.hz.server.ProcessDecisionUnitOfWork;
import ru.taskurotta.service.recovery.RecoveryOperation;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * User: romario
 * Date: 9/12/13
 * Time: 2:28 PM
 */
public class SerializationTest {

    static Random rnd = new Random();
    static Map hzMap;

    @Before
    public void init() {
        Config config = new Config();

        SerializerConfig sc = new SerializerConfig().
                setImplementation(new GraphStreamSerializer()).
                setTypeClass(Graph.class).
                setImplementation(new DecisionRowStreamSerializer()).
                setTypeClass(DecisionRow.class).
                setImplementation(new ArgContainerStreamSerializer()).
                setTypeClass(ArgContainer.class).
                setImplementation(new TaskOptionsContainerSerializer()).
                setTypeClass(TaskOptionsContainer.class).
                setImplementation(new TaskConfigContainerStreamSerializer()).
                setTypeClass(TaskConfigContainer.class).
                setImplementation(new ErrorContainerStreamSerializer()).
                setTypeClass(ErrorContainer.class).
                setImplementation(new TaskContainerStreamSerializer()).
                setTypeClass(TaskContainer.class).
                setImplementation(new RecoveryOperationStreamSerializer()).
                setTypeClass(RecoveryOperation.class).
                setImplementation(new ActorPreferencesStreamSerializer()).
                setTypeClass(ActorPreferences.class).
                setImplementation(new ProcessDecisionUnitOfWorkStreamSerializer()).
                setTypeClass(ProcessDecisionUnitOfWork.class);

        config.getSerializationConfig().addSerializerConfig(sc);

        HazelcastInstance hz = Hazelcast.newHazelcastInstance(ConfigUtil.disableMulticast(config));

        hzMap = hz.getMap("testMap");
    }

    @After
    public void destroy() {
        Hazelcast.shutdownAll();
    }

    @Test
    public void graphSerialization() {
        Graph graph = newRandomGraph();
        hzMap.put(1, graph);
        Graph graphFromMap = (Graph) hzMap.get(1);

        assertEquals(graph, graphFromMap);

        if (graph != graphFromMap) {
            System.out.println("graphFromMap = " + graphFromMap);
        }
    }

    @Test
    public void decisionRowSerializationTest() {
        UUID itemUuid = UUID.randomUUID();
        UUID completeUuid = UUID.randomUUID();
        UUID waitAfterRelease = UUID.randomUUID();

        Modification modification = new Modification();

        Map<UUID, Set<UUID>> links = new HashMap<>();
        Set<UUID> linksSet = new HashSet<>();
        UUID link1 = UUID.randomUUID();
        linksSet.add(link1);
        UUID linkKey = UUID.randomUUID();
        links.put(linkKey, linksSet);

        UUID newItem1 = UUID.randomUUID();
        UUID newItem2 = UUID.randomUUID();


        modification.setCompletedItem(completeUuid);
        modification.setWaitForAfterRelease(waitAfterRelease);
        modification.setLinks(links);
        modification.addNewItem(newItem1);
        modification.addNewItem(newItem2);


        DecisionRow decisionRow = new DecisionRow(itemUuid, completeUuid, modification, null);

        hzMap.put("dec", decisionRow);

        DecisionRow fromMapDecisionRow = (DecisionRow) hzMap.get("dec");

        junit.framework.Assert.assertEquals(decisionRow, fromMapDecisionRow);
    }

    @Test
    public void argContainerSerializationTest() {
        List<ArgContainer> containerList = new ArrayList<>();

        ArgContainer argContainer1 = new ArgContainer();
        argContainer1.setTaskId(UUID.randomUUID());
        argContainer1.setDataType("simple1");
        argContainer1.setJSONValue("jsonData1");
        argContainer1.setPromise(false);
        argContainer1.setReady(true);
        argContainer1.setValueType(ArgContainer.ValueType.COLLECTION);

        containerList.add(argContainer1);

        List<ArgContainer> containerList1 = new ArrayList<>();
        ArgContainer argContainer2 = new ArgContainer();
        argContainer2.setTaskId(UUID.randomUUID());
        argContainer2.setDataType("simple2");
        argContainer2.setJSONValue("jsonData2");
        argContainer2.setPromise(false);
        argContainer2.setReady(true);
        argContainer2.setValueType(ArgContainer.ValueType.COLLECTION);

        containerList1.add(argContainer2);
        ArgContainer[] array1 = new ArgContainer[containerList1.size()];
        containerList1.toArray(array1);

        argContainer1.setCompositeValue(array1);

        ArgContainer argContainer = new ArgContainer();
        argContainer.setDataType("simpleClass");
        argContainer.setJSONValue("jsonData");
        argContainer.setPromise(true);
        argContainer.setValueType(ArgContainer.ValueType.ARRAY);
        argContainer.setReady(false);
        argContainer.setTaskId(UUID.randomUUID());

        ArgContainer[] array = new ArgContainer[containerList.size()];
        containerList.toArray(array);

        argContainer.setCompositeValue(array);
        hzMap.put("argContainer", argContainer);
        ArgContainer getted = (ArgContainer) hzMap.get("argContainer");


        Assert.assertEquals(argContainer.getDataType(), getted.getDataType());
        Assert.assertEquals(argContainer.getJSONValue(), getted.getJSONValue());
        Assert.assertEquals(argContainer.isPromise(), getted.isPromise());
        Assert.assertEquals(argContainer.getValueType(), getted.getValueType());
        Assert.assertEquals(argContainer.isReady(), getted.isReady());
        Assert.assertEquals(argContainer.getTaskId(), getted.getTaskId());
        Assert.assertEquals(argContainer.getCompositeValue()[0].getCompositeValue()[0].getDataType(), "simple2");
    }

    @Test
    public void actorSchedulingOptionsContainerSerializerTest() {
        TaskConfigContainer container = new TaskConfigContainer();
        container.setCustomId("customId");
        container.setStartTime(new Date().getTime());
        container.setTaskList("taskList");
        hzMap.put("actorScheduledOptionsContainer", container);
        TaskConfigContainer getted = (TaskConfigContainer) hzMap.get("actorScheduledOptionsContainer");
        assertEquals(container, getted);
    }

    @Test
    public void errorContainerTest() {
        ErrorContainer errorContainer = new ErrorContainer();
        errorContainer.setClassNames(new String[]{"className"});
        errorContainer.setMessage("message");
        errorContainer.setStackTrace("stack");
        hzMap.put("errorContainer", errorContainer);

        ErrorContainer actual = (ErrorContainer) hzMap.get("errorContainer");

        assertEquals(errorContainer.getClassName(), actual.getClassName());
        assertEquals(errorContainer.getMessage(), actual.getMessage());
        assertEquals(errorContainer.getStackTrace(), actual.getStackTrace());
    }

    @Test
    public void taskContainerTest() {
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

        TaskOptionsContainer taskOptionsContainer = getTaskOptionsContainer();
        UUID processId = UUID.randomUUID();
        String[] failTypes = {"java.lang.RuntimeException"};
        TaskContainer taskContainer = new TaskContainer(taskId, processId, null, method, actorId, type, startTime,
                errorAttempts, args, taskOptionsContainer, true, failTypes);
        hzMap.put("taskContainer", taskContainer);

        TaskContainer actual = (TaskContainer) hzMap.get("taskContainer");

        assertEquals(taskContainer.getArgs()[0].getDataType(), actual.getArgs()[0].getDataType());
        assertEquals(taskContainer.getArgs()[1].getCompositeValue()[0].getDataType(), actual.getArgs()[0].getDataType());
        assertEquals(taskContainer.getMethod(), actual.getMethod());
    }

    @Test
    public void taskOptionsContainerTest() {
        TaskOptionsContainer taskOptionsContainer = getTaskOptionsContainer();
        hzMap.put("taskOptionsContainer", taskOptionsContainer);

        TaskOptionsContainer getted = (TaskOptionsContainer) hzMap.get("taskOptionsContainer");

        assertEquals(taskOptionsContainer.getPromisesWaitFor().length, getted.getPromisesWaitFor().length);
        assertEquals(taskOptionsContainer.getTaskConfigContainer().getStartTime(), getted.getTaskConfigContainer().getStartTime());
        assertEquals(taskOptionsContainer.getTaskConfigContainer().getRetryPolicyConfigContainer().getExceptionsToRetry().contains("java.io.IOException"), true);
        assertEquals(taskOptionsContainer.getTaskConfigContainer().getRetryPolicyConfigContainer().getExceptionsToRetry().contains("java.io.IIOException"), false);
        assertEquals(taskOptionsContainer.getTaskConfigContainer().getRetryPolicyConfigContainer().getExceptionsToExclude().contains("java.lang.IllegalAccessError"), true);
        assertEquals(taskOptionsContainer.getTaskConfigContainer().getRetryPolicyConfigContainer().getType(), getted.getTaskConfigContainer().getRetryPolicyConfigContainer().getType()
        );
        assertEquals(taskOptionsContainer.getArgTypes()[1], getted.getArgTypes()[1]);
    }

    @Test
    public void recoveryOperationSerializationTest() {
        String key = "recoveryOperation";

        RecoveryOperation recoveryOperation = new RecoveryOperation(UUID.randomUUID());
        hzMap.put(key, recoveryOperation);

        RecoveryOperation getted = (RecoveryOperation) hzMap.get(key);
        assertEquals(recoveryOperation, getted);
    }

    @Test
    public void actorPreferencesSerializationTest() {
        ActorPreferences actorPreferences = new ActorPreferences();
        actorPreferences.setId(UUID.randomUUID().toString());
        actorPreferences.setBlocked(false);
        actorPreferences.setQueueName("queueName");
        actorPreferences.setKeepTime(1000l);

        String key = "actorPreferences";
        hzMap.put(key, actorPreferences);

        ActorPreferences getted = (ActorPreferences) hzMap.get(key);
        assertEquals(actorPreferences, getted);
    }

    @Test
    public void processDecisionUnitOfWorkSerializerTest() {
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

        UUID taskId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        String method = "method";
        String actorId = "actorId";
        TaskType type = TaskType.DECIDER_START;
        long startTime = 15121234;
        int errorAttempts = 2;
        String[] failTypes = {"java.lang.RuntimeException"};
        TaskContainer taskContainer = new TaskContainer(taskId, processId, null, method, actorId, type, startTime,
                errorAttempts, args, getTaskOptionsContainer(), true, failTypes);

        TaskKey taskKey = new TaskKey(UUID.randomUUID(), UUID.randomUUID());

        String key = "processDecisionUnitOfWork";
        ProcessDecisionUnitOfWork processDecisionUnitOfWork = new ProcessDecisionUnitOfWork(taskKey);
        hzMap.put(key, processDecisionUnitOfWork);

        ProcessDecisionUnitOfWork getted = (ProcessDecisionUnitOfWork) hzMap.get(key);
        assertEquals(processDecisionUnitOfWork, getted);
    }

    public static TaskOptionsContainer getTaskOptionsContainer() {
        TaskConfigContainer container = new TaskConfigContainer();
        container.setCustomId("customId");
        container.setStartTime(new Date().getTime());
        container.setTaskList("taskList");

        RetryPolicyConfigContainer retryPolicyConfig = new RetryPolicyConfigContainer();
        retryPolicyConfig.setType(RetryPolicyConfig.RetryPolicyType.LINEAR);
        retryPolicyConfig.setMaximumAttempts(1);
        retryPolicyConfig.setInitialRetryIntervalSeconds(5);
        retryPolicyConfig.setMaximumRetryIntervalSeconds(25);
        retryPolicyConfig.setRetryExpirationIntervalSeconds(5);
        retryPolicyConfig.setBackoffCoefficient(1.1);
        retryPolicyConfig.addExceptionToRetryException(IndexOutOfBoundsException.class);
        retryPolicyConfig.addExceptionToRetryException(IOException.class);
        retryPolicyConfig.addExceptionToRetryException(IllegalFormatException.class);
        retryPolicyConfig.addExceptionToExclude(IllegalAccessError.class);
        retryPolicyConfig.addExceptionToExclude(IllegalArgumentException.class);

        container.setRetryPolicyConfigContainer(retryPolicyConfig);

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
        argContainer2.setDataType("simple2");
        argContainer2.setJSONValue("jsonData2");
        argContainer2.setPromise(false);
        argContainer2.setReady(true);
        argContainer2.setValueType(ArgContainer.ValueType.COLLECTION);

        containerList.add(argContainer1);
        containerList.add(argContainer2);

        ArgContainer[] array = new ArgContainer[containerList.size()];
        containerList.toArray(array);

        return new TaskOptionsContainer(new ArgType[]{ArgType.NO_WAIT, ArgType.WAIT}, container, array);
    }

    public static Graph newRandomGraph() {

        Map<UUID, Long> notFinishedItems = new HashMap<>();
        notFinishedItems.put(UUID.randomUUID(), System.currentTimeMillis());
        notFinishedItems.put(UUID.randomUUID(), System.currentTimeMillis());
        notFinishedItems.put(UUID.randomUUID(), System.currentTimeMillis());
        notFinishedItems.put(UUID.randomUUID(), System.currentTimeMillis());
        notFinishedItems.put(UUID.randomUUID(), System.currentTimeMillis());
        notFinishedItems.put(UUID.randomUUID(), System.currentTimeMillis());

        Map<UUID, Set<UUID>> links = new HashMap<>();
        Set<UUID> set1 = new HashSet<>();
        set1.add(UUID.randomUUID());
        set1.add(UUID.randomUUID());
        links.put(UUID.randomUUID(), set1);
        Set<UUID> set2 = new HashSet<>();
        set1.add(UUID.randomUUID());
        set1.add(UUID.randomUUID());
        links.put(UUID.randomUUID(), set1);

        Set<UUID> finishedItems = new HashSet<>();
        finishedItems.add(UUID.randomUUID());
        finishedItems.add(UUID.randomUUID());


        return new Graph(rnd.nextInt(), UUID.randomUUID(), notFinishedItems, links, finishedItems, System
                .currentTimeMillis(), System.currentTimeMillis());
    }

}
