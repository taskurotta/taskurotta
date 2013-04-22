package ru.taskurotta.backend.ora.dependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

import static ru.taskurotta.backend.ora.tools.SqlResourceCloser.closeResources;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.links.Modification;
import ru.taskurotta.backend.storage.model.serialization.JsonSerializer;

/**
 * User: moroz
 * Date: 17.04.13
 */
public class OraGraphDao implements GraphDao {

    private static ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(OraGraphDao.class);
    private static final JsonSerializer<Graph> graphJsonSerializer = new JsonSerializer<Graph>(Graph.class);
    private static final JsonSerializer<Modification> modificationJsonSerializer = new JsonSerializer<Modification>(Modification.class);
    private static final JsonSerializer<UUID[]> itemsJsonSerializer = new JsonSerializer<UUID[]>(UUID[].class);

    private DataSource dataSource;

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public void createGraph(UUID graphId, UUID taskId) {
        Graph graph = new Graph(graphId, taskId);
        insertUpdateGraph(graph);
    }

    private void insertUpdateGraph(Graph graph) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("MERGE INTO GRAPH USING dual ON (id=? )\n" +
                    "        WHEN MATCHED THEN UPDATE SET version=? , JSON_STR = ?\n" +
                    "        WHEN NOT MATCHED THEN INSERT (ID, VERSION, JSON_STR)\n" +
                    "        VALUES ( ?, ?, ?)");

            ps.setString(1, graph.getGraphId().toString());
            ps.setInt(2, graph.getVersion());
            ps.setString(3, (String) graphJsonSerializer.serialize(graph));
            ps.setString(4, graph.getGraphId().toString());
            ps.setInt(5, graph.getVersion());
            ps.setString(6, (String) graphJsonSerializer.serialize(graph));
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    @Override
    public Graph getGraph(UUID graphId) {
        Connection connection = null;
        PreparedStatement ps = null;
        Graph result = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("SELECT  JSON_STR FROM GRAPH WHERE ID = ?");
            ps.setString(1, graphId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String json = rs.getString(1);
                result = graphJsonSerializer.deserialize(json);
            }

        } catch (SQLException ex) {
            logger.error("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
        return result;
    }

    @Override
    public boolean updateGraph(Graph modifiedGraph) {
        logger.debug("updateGraph() modifiedGraph = [{}]", modifiedGraph);

        Connection connection = null;
        PreparedStatement ps = null;
        boolean result = false;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("insert into GRAPH_DECISION(GRAPH_ID, READY_ITEMS, MODIFICATION_JSON) values (?,?,?)");
            ps.setString(1, modifiedGraph.getModification().getCompletedItem().toString());
            ps.setString(2, (String) itemsJsonSerializer.serialize(modifiedGraph.getReadyItems()));
            ps.setString(3, (String) modificationJsonSerializer.serialize(modifiedGraph.getModification()));
            ps.executeUpdate();

            Graph graph = getGraph(modifiedGraph.getGraphId());
            int newVersion = modifiedGraph.getVersion();

            if (graph.getVersion() == newVersion - 1) {
                insertUpdateGraph(graph);
                result = true;
            }

        } catch (SQLException ex) {
            logger.error("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
        return result;

    }

    @Override
    public UUID[] getReadyTasks(UUID finishedTaskId) {
        UUID[] result = new UUID[0];
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("SELECT  READY_ITEMS FROM GRAPH_DECISION WHERE GRAPH_ID = ?");
            ps.setString(1, finishedTaskId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String json = rs.getString(1);
                result = itemsJsonSerializer.deserialize(json);
            }

        } catch (SQLException ex) {
            logger.error("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
        return result;
    }
}
