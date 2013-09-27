package ru.taskurotta.backend.hz.serialization;

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
import ru.taskurotta.transport.model.ArgContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
                setTypeClass(HzGraphDao.DecisionRow.class).
                setImplementation(new ArgContainerSerializer()).
                setTypeClass(ArgContainer.class);

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

        assertEquals(decisionRow, fromMapDecisionRow);
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
