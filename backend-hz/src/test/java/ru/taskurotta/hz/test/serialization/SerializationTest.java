package ru.taskurotta.hz.test.serialization;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.Modification;
import ru.taskurotta.backend.hz.dependency.HzGraphDao;
import ru.taskurotta.backend.hz.serialization.ActorSchedulingOptionsContainerStreamSerializer;
import ru.taskurotta.backend.hz.serialization.ArgContainerStreamSerializer;
import ru.taskurotta.backend.hz.serialization.DecisionRowStreamSerializer;
import ru.taskurotta.backend.hz.serialization.ErrorContainerStreamSerializer;
import ru.taskurotta.backend.hz.serialization.GraphStreamSerializer;
import ru.taskurotta.backend.hz.serialization.TaskContainerStreamSerializer;
import ru.taskurotta.backend.hz.serialization.TaskOptionsContainerSerializer;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ArgType;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.model.TaskType;

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
                setTypeClass(HzGraphDao.DecisionRow.class).
                setImplementation(new ArgContainerStreamSerializer()).
                setTypeClass(ArgContainer.class).
                setImplementation(new TaskOptionsContainerSerializer()).
                setTypeClass(TaskOptionsContainer.class).
                setImplementation(new ActorSchedulingOptionsContainerStreamSerializer()).
                setTypeClass(ActorSchedulingOptionsContainer.class).
                setImplementation(new ErrorContainerStreamSerializer()).
                setTypeClass(ErrorContainer.class).
                setImplementation(new TaskContainerStreamSerializer()).
                setTypeClass(TaskContainer.class);


        config.getSerializationConfig().addSerializerConfig(sc);

        HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
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


        HzGraphDao.DecisionRow decisionRow = new HzGraphDao.DecisionRow(itemUuid, modification, null);

        hzMap.put("dec", decisionRow);

        HzGraphDao.DecisionRow fromMapDecisionRow = (HzGraphDao.DecisionRow) hzMap.get("dec");

        junit.framework.Assert.assertEquals(decisionRow, fromMapDecisionRow);
    }

    @Test
    public void argContainerSerializationTest() {
        List<ArgContainer> containerList = new ArrayList<>();

        ArgContainer argContainer1 = new ArgContainer();
        argContainer1.setTaskId(UUID.randomUUID());
        argContainer1.setClassName("simple1");
        argContainer1.setJSONValue("jsonData1");
        argContainer1.setPromise(false);
        argContainer1.setReady(true);
        argContainer1.setType(ArgContainer.ValueType.COLLECTION);

        containerList.add(argContainer1);

        List<ArgContainer> containerList1 = new ArrayList<>();
        ArgContainer argContainer2 = new ArgContainer();
        argContainer2.setTaskId(UUID.randomUUID());
        argContainer2.setClassName("simple2");
        argContainer2.setJSONValue("jsonData2");
        argContainer2.setPromise(false);
        argContainer2.setReady(true);
        argContainer2.setType(ArgContainer.ValueType.COLLECTION);

        containerList1.add(argContainer2);
        ArgContainer[] array1 = new ArgContainer[containerList1.size()];
        containerList1.toArray(array1);

        argContainer1.setCompositeValue(array1);

        ArgContainer argContainer = new ArgContainer();
        argContainer.setClassName("simpleClass");
        argContainer.setJSONValue("jsonData");
        argContainer.setPromise(true);
        argContainer.setType(ArgContainer.ValueType.ARRAY);
        argContainer.setReady(false);
        argContainer.setTaskId(UUID.randomUUID());

        ArgContainer[] array = new ArgContainer[containerList.size()];
        containerList.toArray(array);

        argContainer.setCompositeValue(array);
        hzMap.put("argContainer", argContainer);
        ArgContainer getted = (ArgContainer) hzMap.get("argContainer");


        Assert.assertEquals(argContainer.getClassName(), getted.getClassName());
        Assert.assertEquals(argContainer.getJSONValue(), getted.getJSONValue());
        Assert.assertEquals(argContainer.isPromise(), getted.isPromise());
        Assert.assertEquals(argContainer.getType(), getted.getType());
        Assert.assertEquals(argContainer.isReady(), getted.isReady());
        Assert.assertEquals(argContainer.getTaskId(), getted.getTaskId());
        Assert.assertEquals(argContainer.getCompositeValue()[0].getCompositeValue()[0].getClassName(), "simple2");
    }

    @Test
    public void actorSchedulingOptionsContainerSerializerTest() {
        ActorSchedulingOptionsContainer container = new ActorSchedulingOptionsContainer();
        container.setCustomId("customId");
        container.setStartTime(new Date().getTime());
        container.setTaskList("taskList");
        hzMap.put("actorScheduledOptionsContainer", container);
        ActorSchedulingOptionsContainer getted = (ActorSchedulingOptionsContainer) hzMap.get("actorScheduledOptionsContainer");
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
        int numberOfAttempts = 2;

        List<ArgContainer> containerList = new ArrayList<>();
        ArgContainer argContainer1 = new ArgContainer();
        argContainer1.setTaskId(UUID.randomUUID());
        argContainer1.setClassName("simple1");
        argContainer1.setJSONValue("jsonData1");
        argContainer1.setPromise(false);
        argContainer1.setReady(true);
        argContainer1.setType(ArgContainer.ValueType.COLLECTION);

        ArgContainer argContainer2 = new ArgContainer();
        argContainer2.setTaskId(UUID.randomUUID());
        argContainer2.setClassName("simple1");
        argContainer2.setJSONValue("jsonData1");
        argContainer2.setPromise(false);
        argContainer2.setReady(true);
        argContainer2.setType(ArgContainer.ValueType.COLLECTION);

        argContainer2.setCompositeValue(new ArgContainer[]{argContainer1});

        containerList.add(argContainer1);
        containerList.add(argContainer2);

        ArgContainer[] args = new ArgContainer[containerList.size()];
        containerList.toArray(args);

        TaskOptionsContainer taskOptionsContainer = getTaskOptionsContainer();
        UUID processId = UUID.randomUUID();
        String[] failTypes = {"java.lang.RuntimeException"};
        TaskContainer taskContainer = new TaskContainer(taskId, processId, method, actorId, type, startTime, numberOfAttempts, args, taskOptionsContainer, true, failTypes);
        hzMap.put("taskContainer", taskContainer);

        TaskContainer actual = (TaskContainer) hzMap.get("taskContainer");

        assertEquals(taskContainer.getArgs()[0].getClassName(), actual.getArgs()[0].getClassName());
        assertEquals(taskContainer.getArgs()[1].getCompositeValue()[0].getClassName(), actual.getArgs()[0].getClassName());
        assertEquals(taskContainer.getMethod(), actual.getMethod());
    }

    @Test
    public void taskOptionsContainerTest() {
        TaskOptionsContainer taskOptionsContainer = getTaskOptionsContainer();
        hzMap.put("taskOptionsContainer", taskOptionsContainer);

        TaskOptionsContainer getted = (TaskOptionsContainer) hzMap.get("taskOptionsContainer");

        assertEquals(taskOptionsContainer.getPromisesWaitFor().length, getted.getPromisesWaitFor().length);
        assertEquals(taskOptionsContainer.getActorSchedulingOptions().getStartTime(), getted.getActorSchedulingOptions().getStartTime());
        assertEquals(taskOptionsContainer.getArgTypes()[1], getted.getArgTypes()[1]);

    }

    private TaskOptionsContainer getTaskOptionsContainer() {
        ActorSchedulingOptionsContainer container = new ActorSchedulingOptionsContainer();
        container.setCustomId("customId");
        container.setStartTime(new Date().getTime());
        container.setTaskList("taskList");

        List<ArgContainer> containerList = new ArrayList<>();

        ArgContainer argContainer1 = new ArgContainer();
        argContainer1.setTaskId(UUID.randomUUID());
        argContainer1.setClassName("simple1");
        argContainer1.setJSONValue("jsonData1");
        argContainer1.setPromise(false);
        argContainer1.setReady(true);
        argContainer1.setType(ArgContainer.ValueType.COLLECTION);

        ArgContainer argContainer2 = new ArgContainer();
        argContainer2.setTaskId(UUID.randomUUID());
        argContainer2.setClassName("simple2");
        argContainer2.setJSONValue("jsonData2");
        argContainer2.setPromise(false);
        argContainer2.setReady(true);
        argContainer2.setType(ArgContainer.ValueType.COLLECTION);

        containerList.add(argContainer1);
        containerList.add(argContainer2);

        ArgContainer[] array = new ArgContainer[containerList.size()];
        containerList.toArray(array);

        return new TaskOptionsContainer(new ArgType[]{ArgType.NO_WAIT, ArgType.WAIT}, container, array);
    }

    private static Graph newRandomGraph() {

        Map<UUID, Long> notFinishedItems = new HashMap<>();
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


        return new Graph(rnd.nextInt(), UUID.randomUUID(), notFinishedItems, links, finishedItems, System.currentTimeMillis());
    }

}
