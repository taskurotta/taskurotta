package ru.taskurotta.service.hz.dependency;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.dependency.links.Modification;
import ru.taskurotta.service.hz.TaskKey;

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
    private IMap<TaskKey, DecisionRow> decisions;

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
        private UUID taskId;
        private UUID processId;
        private Modification modification;
        private UUID[] readyItems;

        public DecisionRow(UUID taskId, UUID processId, Modification modification, UUID[] readyItems) {
            this.taskId = taskId;
            this.processId = processId;
            this.modification = modification;
            this.readyItems = readyItems;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DecisionRow)) return false;

            DecisionRow that = (DecisionRow) o;

            if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
            if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
            if (modification != null ? !modification.equals(that.modification) : that.modification != null)
                return false;
            if (!Arrays.equals(readyItems, that.readyItems)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = taskId != null ? taskId.hashCode() : 0;
            result = 31 * result + (processId != null ? processId.hashCode() : 0);
            result = 31 * result + (modification != null ? modification.hashCode() : 0);
            result = 31 * result + (readyItems != null ? Arrays.hashCode(readyItems) : 0);
            return result;
        }

        public UUID getTaskId() {
            return taskId;
        }

        public UUID getProcessId() {
            return processId;
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
                    "taskId=" + taskId +
                    ", processId=" + processId +
                    ", modification=" + modification +
                    ", readyItems=" + Arrays.toString(readyItems) +
                    '}';
        }
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

        for (UUID itemId : finishedItems) {
            decisions.delete(new TaskKey(itemId, graphId));
        }

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
    public UUID[] getReadyTasks(UUID finishedTaskId, UUID processId) {
        DecisionRow decisionRow = decisions.get(new TaskKey(finishedTaskId, processId));
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
