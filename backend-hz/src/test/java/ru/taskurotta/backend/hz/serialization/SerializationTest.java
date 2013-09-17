package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.backend.dependency.links.Graph;

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
                setTypeClass(Graph.class);

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
