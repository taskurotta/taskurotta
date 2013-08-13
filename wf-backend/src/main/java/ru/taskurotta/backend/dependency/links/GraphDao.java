package ru.taskurotta.backend.dependency.links;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/8/13
 * Time: 10:23 AM
 */
public interface GraphDao {

    /**
     * @param graphId
     * @return last version of graph links.
     */
    public Graph getGraph(UUID graphId);


    /**
     * Saves new version of graph
     *
     * @param modifiedGraph
     * @return true if graph saved successfully.
     */
    public boolean updateGraph(Graph modifiedGraph);

    /**
     * Register new process graph.
     *
     * @param graphId
     * @param taskId
     */
    public void createGraph(UUID graphId, UUID taskId);


    /**
     * Returns array of ready tasks by finishedTaskId from history for replay case.
     *
     * @param finishedTaskId
     * @return
     */
    public UUID[] getReadyTasks(UUID finishedTaskId);


    /**
     * Run graph updater
     *
     * @param updater
     * @return true when update are completed successfully
     */
    boolean changeGraph(Updater updater);


    /**
     * This class delegates synchronization solution to the DAO level.
     */
    public interface Updater {

        public UUID getProcessId();

        /**
         * @param graph
         * @return true if DAO should update graph in storage
         */
        public boolean apply(Graph graph);

    }
}
