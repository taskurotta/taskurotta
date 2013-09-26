package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.Modification;
import ru.taskurotta.backend.hz.dependency.HzGraphDao;

import java.util.HashMap;
import java.util.HashSet;
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
                setTypeClass(HzGraphDao.DecisionRow.class);

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
