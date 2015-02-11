package ru.taskurotta.service.hz.dependency;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.dependency.links.Modification;

import java.io.Serializable;
import java.util.Arrays;
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
    private IMap<UUID, DecisionRow> decisions;

    public HzGraphDao(HazelcastInstance hzInstance, String graphsMapName, String decisionsMapName) {
        this.graphs = hzInstance.getMap(graphsMapName);
        this.decisions = hzInstance.getMap(decisionsMapName);
    }

    public HzGraphDao(HazelcastInstance hzInstance) {
        this(hzInstance, "graphsMapName", "graphDecisionsMapName");
    }

    /**
     * Table of row contains decision stuff
     */
    public static class DecisionRow implements Serializable {
        private UUID itemId;
        private Modification modification;
        private UUID[] readyItems;

        public DecisionRow(UUID itemId, Modification modification, UUID[] readyItems) {
            this.itemId = itemId;
            this.modification = modification;
            this.readyItems = readyItems;
        }

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

        public UUID getItemId() {
            return itemId;
        }

        public Modification getModification() {
            return modification;
        }

        public UUID[] getReadyItems() {
            return readyItems;
        }

        @Override
        public String toString() {
            return "DecisionRow{" +
                    "itemId=" + itemId +
                    ", modification=" + modification +
                    ", readyItems=" + Arrays.toString(readyItems) +
                    '}';
        }
    }


    @Override
    public void createGraph(UUID graphId, UUID taskId) {

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

        Graph graph = graphs.get(graphId);

        Set<UUID> finishedItems = graph.getFinishedItems();

        for (UUID itemId : finishedItems) {
            decisions.delete(itemId);
        }

        graphs.delete(graphId);
    }

    @Override
    public Graph getGraph(UUID graphId) {
        return graphs.get(graphId);
    }

    private boolean updateGraph(Graph modifiedGraph) {
        logger.debug("updateGraph() modifiedGraph = [{}]", modifiedGraph);

        Modification modification = modifiedGraph.getModification();

        if (modification != null) {
            DecisionRow decisionRow = new DecisionRow(modification.getCompletedItem(), modification, modifiedGraph.getReadyItems());

            decisions.set(decisionRow.itemId, decisionRow, 0, TimeUnit.NANOSECONDS);
        }

        graphs.set(modifiedGraph.getGraphId(), modifiedGraph, 0, TimeUnit.NANOSECONDS);//hz feature

        return true;
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

}
