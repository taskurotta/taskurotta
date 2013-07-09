package ru.taskurotta.backend.hz.dependency;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.links.Modification;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Memory graph dao with Hazelcast
 * User: dimadin
 * Date: 13.06.13 14:40
 */
public class HzGraphDao implements GraphDao {

    private final static Logger logger = LoggerFactory.getLogger(HzGraphDao.class);

    // TODO: garbage collection policy for real database

    private Map<UUID, GraphRow> graphs;
    private Map<UUID, DecisionRow> decisions;
    private ILock graphLock;

    public HzGraphDao(HazelcastInstance hzInstance, String graphsMapName, String decisionsMapName) {
        this.graphs = hzInstance.getMap(graphsMapName);
        this.decisions = hzInstance.getMap(decisionsMapName);
        graphLock = hzInstance.getLock(graphs);
    }

    public HzGraphDao(HazelcastInstance hzInstance) {
        this(hzInstance, "graphsMapName", "decisionsMapName");
    }

    /**
     * Table of row contains current graph (process) state
     */
    public static class GraphRow implements Serializable {
        private int version;
        private Graph graph;

        protected GraphRow(Graph graph) {
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
            graphLock.lock();
            if (graphs.get(graphId) != null) {
                return;
            }

            Graph graph = new Graph(graphId, taskId);
            GraphRow graphRow = new GraphRow(graph);

            graphs.put(graphId, graphRow);

        } finally {
            graphLock.unlock();
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

        decisions.put(decisionRow.itemId, decisionRow);

        GraphRow graphRow = graphs.get(modifiedGraph.getGraphId());
        boolean result = graphRow.updateGraph(modifiedGraph);
        graphs.put(modifiedGraph.getGraphId(), graphRow);//hz feature
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

}
