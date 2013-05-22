package ru.taskurotta.backend.ora.dependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.links.Modification;
import ru.taskurotta.transport.model.serialization.JsonSerializer;
import ru.taskurotta.exception.BackendCriticalException;

/**
 * User: moroz
 * Date: 17.04.13
 */
public class OraGraphDao implements GraphDao {

    private static final Logger logger = LoggerFactory.getLogger(OraGraphDao.class);
    private static final JsonSerializer<Graph> graphJsonSerializer = new JsonSerializer<Graph>(Graph.class);
    private static final JsonSerializer<Modification> modificationJsonSerializer = new JsonSerializer<Modification>(Modification.class);
    private static final JsonSerializer<UUID[]> itemsJsonSerializer = new JsonSerializer<UUID[]>(UUID[].class);

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public void createGraph(UUID graphId, UUID taskId) {
        Graph graph = new Graph(graphId, taskId);
        insertUpdateGraph(graph);
    }

    private void insertUpdateGraph(Graph graph) {
        logger.debug("insertUpdateGraph(graph) with graph [{}]", graph);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("MERGE INTO GRAPH USING dual ON (id=? )\n" +
                     "        WHEN MATCHED THEN UPDATE SET version=? , JSON_STR = ? WHERE  version=?\n" +
                     "        WHEN NOT MATCHED THEN INSERT (ID, VERSION, JSON_STR)\n" +
                     "        VALUES ( ?, ?, ?)")
        ) {
            ps.setString(1, graph.getGraphId().toString());
            ps.setInt(2, graph.getVersion());
            ps.setString(3, (String) graphJsonSerializer.serialize(graph));
            ps.setInt(4, graph.getVersion() - 1);
            ps.setString(5, graph.getGraphId().toString());
            ps.setInt(6, graph.getVersion());
            ps.setString(7, (String) graphJsonSerializer.serialize(graph));
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }

    @Override
    public Graph getGraph(UUID graphId) {
        Graph result = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT  JSON_STR FROM GRAPH WHERE ID = ?")
        ) {
            ps.setString(1, graphId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String json = rs.getString(1);
                result = graphJsonSerializer.deserialize(json);
            }

        } catch (SQLException ex) {
            logger.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        }
        return result;
    }

    @Override
    public boolean updateGraph(Graph modifiedGraph) {
        logger.debug("updateGraph() modifiedGraph = [{}]", modifiedGraph);
        boolean result = false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("call  add_graph_decision(?,?,?)")
        ) {
            ps.setString(1, modifiedGraph.getModification().getCompletedItem().toString());
            ps.setString(2, (String) itemsJsonSerializer.serialize(modifiedGraph.getReadyItems()));
            ps.setString(3, (String) modificationJsonSerializer.serialize(modifiedGraph.getModification()));
            ps.executeUpdate();

            Graph graph = getGraph(modifiedGraph.getGraphId());
            int newVersion = modifiedGraph.getVersion();

            if (graph.getVersion() == newVersion - 1) {
                insertUpdateGraph(modifiedGraph);
                result = true;
            }
        } catch (SQLException ex) {
            logger.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        }
        logger.debug("Update graph result is [{}]", result);
        return result;

    }

    @Override
    public UUID[] getReadyTasks(UUID finishedTaskId) {
        UUID[] result = new UUID[0];
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT  READY_ITEMS FROM GRAPH_DECISION WHERE FINISHED_TASK_ID = ?")
        ) {
            ps.setString(1, finishedTaskId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String json = rs.getString(1);
                result = itemsJsonSerializer.deserialize(json);
            }

        } catch (SQLException ex) {
            logger.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        }
        return result;
    }
}
