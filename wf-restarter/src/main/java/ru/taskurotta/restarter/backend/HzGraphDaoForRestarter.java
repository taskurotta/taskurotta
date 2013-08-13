package ru.taskurotta.restarter.backend;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.hz.dependency.HzGraphDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 12.08.13
 * Time: 18:19
 */
public class HzGraphDaoForRestarter extends HzGraphDao {

    public HzGraphDaoForRestarter(HazelcastInstance hzInstance, String graphsMapName, String decisionsMapName, boolean createGraphLock) {
        super(hzInstance, graphsMapName, decisionsMapName);
    }

    @Override
    public void createGraph(UUID graphId, UUID taskId) {
        if (graphs.get(graphId) != null) {
            return;
        }

        Graph graph = new Graph(graphId, taskId);

        graphs.set(graphId, graph, 0, TimeUnit.NANOSECONDS);
    }
}
