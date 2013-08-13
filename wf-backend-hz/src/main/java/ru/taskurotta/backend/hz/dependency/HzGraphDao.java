package ru.taskurotta.backend.hz.dependency;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.links.Modification;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Memory graph dao with Hazelcast
 * User: dimadin
 * Date: 13.06.13 14:40
 */
public class HzGraphDao implements GraphDao {

    private final static Logger logger = LoggerFactory.getLogger(HzGraphDao.class);

    // TODO: garbage collection policy for real database

    protected IMap<UUID, GraphRow> graphs;
    private IMap<UUID, DecisionRow> decisions;

    public HzGraphDao(HazelcastInstance hzInstance, String graphsMapName, String decisionsMapName) {
        this.graphs = hzInstance.getMap(graphsMapName);
        this.decisions = hzInstance.getMap(decisionsMapName);
    }

    public HzGraphDao(HazelcastInstance hzInstance) {
        this(hzInstance, "graphsMapName", "graphDecisionsMapName");
    }

    /**
     * Table of row contains current graph (process) state
     */
    public static class GraphRow implements Serializable {
        private int version;
        private Graph graph;

        public GraphRow(Graph graph) {
            version = graph.getVersion();
            this.graph = graph;
        }


        /**
         * @param modifiedGraph - new version of the graph
         * @return true if modification was successful
         */
        protected boolean updateGraph(Graph modifiedGraph) {

            int newVersion = modifiedGraph.getVersion();

            if (version != newVersion - 1) {
                return false;
            }
            version = newVersion;
            graph = modifiedGraph;

            return true;
        }

    }

    /**
     * Table of row contains decision stuff
     */
    private static class DecisionRow implements Serializable {
        private UUID itemId;
        private Modification modification;
        private UUID[] readyItems;

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DecisionRow)) return false;

            DecisionRow that = (DecisionRow) o;

            if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) return false;
            if (modification != null ? !modification.equals(that.modification) : that.modification != null)
                return false;
            if (!Arrays.equals(readyItems, that.readyItems)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = itemId != null ? itemId.hashCode() : 0;
            result = 31 * result + (modification != null ? modification.hashCode() : 0);
            result = 31 * result + (readyItems != null ? Arrays.hashCode(readyItems) : 0);
            return result;
        }
    }


    @Override
    public void createGraph(UUID graphId, UUID taskId) {
        if (graphs.get(graphId) != null) {
            return;
        }

        try {
            graphs.lock(graphId);
            if (graphs.get(graphId) != null) {
                return;
            }

            Graph graph = new Graph(graphId, taskId);
            GraphRow graphRow = new GraphRow(graph);

            graphs.set(graphId, graphRow, 0, TimeUnit.NANOSECONDS);

        } finally {
            graphs.unlock(graphId);
        }

    }

    @Override
    public Graph getGraph(UUID graphId) {

        GraphRow graphRow = graphs.get(graphId);

        if (graphRow == null) {
            return null;
        }

        return graphRow.graph;
    }

    @Override
    public boolean updateGraph(Graph modifiedGraph) {
        logger.debug("updateGraph() modifiedGraph = [{}]", modifiedGraph);

        DecisionRow decisionRow = new DecisionRow();

        Modification modification = modifiedGraph.getModification();
        decisionRow.itemId = modifiedGraph.getModification().getCompletedItem();
        decisionRow.modification = modification;
        decisionRow.readyItems = modifiedGraph.getReadyItems();

        decisions.set(decisionRow.itemId, decisionRow, 0, TimeUnit.NANOSECONDS);

        GraphRow graphRow = graphs.get(modifiedGraph.getGraphId());
        boolean result = graphRow.updateGraph(modifiedGraph);
        graphs.set(modifiedGraph.getGraphId(), graphRow, 0, TimeUnit.NANOSECONDS);//hz feature
        return result;

    }


    @Override
    public UUID[] getReadyTasks(UUID finishedTaskId) {
        DecisionRow decisionRow = decisions.get(finishedTaskId);
        if (decisionRow != null) {
            return decisionRow.readyItems;
        }

        return Graph.EMPTY_ARRAY;
    }

    @Override
    public boolean changeGraph(Updater updater) {

        UUID graphId = updater.getProcessId();

        try {
            graphs.lock(graphId);

            GraphRow graphRow = graphs.get(graphId);

            if (graphRow == null) {
                return false;
            }

            if (updater.apply(graphRow.graph)) {
                return updateGraph(graphRow.graph);
            }

        } finally {
            graphs.unlock(graphId);
        }

        return false;
    }

}
