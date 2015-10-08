package ru.taskurotta.service.hz.dependency;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Memory graph dao with Hazelcast
 * User: dimadin
 * Date: 13.06.13 14:40
 */
public class HzGraphDao implements GraphDao {

    private final static Logger logger = LoggerFactory.getLogger(HzGraphDao.class);

    protected IMap<UUID, Graph> graphs;

    public HzGraphDao(HazelcastInstance hzInstance, String graphsMapName) {
        this.graphs = hzInstance.getMap(graphsMapName);
    }

    public HzGraphDao(HazelcastInstance hzInstance) {
        this(hzInstance, "graphsMapName");
    }


    @Override
    public void createGraph(UUID graphId, UUID taskId) {

        logger.debug("Create graph {}", graphId);

        try {
            graphs.lock(graphId);

            Graph graph = new Graph(graphId, taskId);

            graphs.set(graphId, graph, 0, TimeUnit.NANOSECONDS);

        } finally {
            graphs.unlock(graphId);
        }
    }

    @Override
    public void deleteGraph(UUID graphId) {

        logger.debug("Delete graph {}", graphId);

        Graph graph = graphs.get(graphId);
        if (graph == null) {
            logger.warn("Graph {} can not be removed because it is not found", graphId);
            return;
        }

        Set<UUID> finishedItems = graph.getFinishedItems();

        graphs.delete(graphId);
    }

    @Override
    public Graph getGraph(UUID graphId) {
        return graphs.get(graphId);
    }

    private boolean updateGraph(Graph modifiedGraph) {
        logger.debug("updateGraph() modifiedGraph = [{}]", modifiedGraph);

//        Modification modification = modifiedGraph.getModification();
//
//        if (modification != null) {
//            TaskKey taskKey = new TaskKey(modification.getCompletedItem(), modifiedGraph.getGraphId());
//
//            DecisionRow decisionRow = new DecisionRow(taskKey.getTaskId(), taskKey.getProcessId(),
//                    modification, modifiedGraph.getReadyItems());
//
//            decisions.set(taskKey, decisionRow, 0, TimeUnit.NANOSECONDS);
//
//            modifiedGraph.removeModification();
//        }



        graphs.set(modifiedGraph.getGraphId(), modifiedGraph, 0, TimeUnit.NANOSECONDS);//hz feature

        return true;
    }


    @Override
    public boolean changeGraph(Updater updater) {

        UUID graphId = updater.getProcessId();

        try {
            graphs.lock(graphId);

            Graph graph = graphs.get(graphId);

            if (graph == null) {
                return false;
            }

            if (updater.apply(graph)) {
                return updateGraph(graph);
            }

        } finally {
            graphs.unlock(graphId);
        }

        return false;
    }

    @Override
    public ResultSetCursor<UUID> findLostGraphs(long lastGraphChangeTime, int batchSize) {
        throw new UnsupportedOperationException("Please, use MongoGraphDao");
    }

}
