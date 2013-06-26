package ru.taskurotta.backend.snapshot.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.snapshot.Snapshot;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskOptionsImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: greg
 */
public class JDBCSnapshotDataSource implements SnapshotDataSource {

    protected final static Logger log = LoggerFactory.getLogger(JDBCSnapshotDataSource.class);

    private DataSource dataSource;

    private ObjectMapper mapper;

    public JDBCSnapshotDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addAbstractTypeMapping(Task.class, TaskImpl.class);
        module.addAbstractTypeMapping(TaskDecision.class, TaskDecisionImpl.class);
        module.addAbstractTypeMapping(TaskOptions.class, TaskOptionsImpl.class);
        module.addAbstractTypeMapping(TaskTarget.class, TaskTargetImpl.class);
        mapper.registerModule(module);
    }

    @Override
    public List<Snapshot> getSnapshotsForPeriod(Date startDate, Date endDate) {
        final String sql = "select * from snapshot where created_date between ? and ?";
        ResultSet rs = null;
        List<Snapshot> list = new ArrayList<>();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setObject(1, startDate);
            statement.setObject(2, endDate);
            rs = statement.executeQuery();
            while (rs.next()) {
                list.add(buildSnapshot(rs));
            }

        } catch (SQLException ex) {
            log.error("sql error", ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.error("result set closing problem", e);
                }
            }
        }
        return list;
    }

    private Snapshot buildSnapshot(ResultSet rs) throws SQLException {
        Snapshot snapshot = new Snapshot();
        try {
            snapshot.setGraph(mapper.readValue(rs.getString("graph"), Graph.class));

            snapshot.setSnapshotId((UUID) rs.getObject("snapshotId"));
            snapshot.setTask(mapper.readValue(rs.getString("task"), Task.class));
            snapshot.setTaskDecision(mapper.readValue(rs.getString("task_decision"), TaskDecision.class));
            snapshot.setCreatedDate(rs.getDate("created_date"));
        } catch (IOException e) {
            log.error("deserialization error", e);
        }
        return snapshot;
    }

    @Override
    public Snapshot loadSnapshotById(UUID id) {
        final String sql = "select * from snapshot where snapshotId=?";
        ResultSet rs = null;
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setObject(1, id);
            rs = statement.executeQuery();
            if (rs.next()) {
                return buildSnapshot(rs);
            }
        } catch (SQLException ex) {
            log.error("sql error", ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.error("result set closing problem", e);
                }
            }
        }
        return null;
    }

    @Override
    public void save(Snapshot snapshot) {
        final String sql = "insert into snapshot(snapshotId, task, graph, task_decision, created_date) values(?, ?, ?, ?, ?)";
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setObject(1, snapshot.getSnapshotId());
            statement.setString(2, mapper.writeValueAsString(snapshot.getTask()));
            statement.setString(3, mapper.writeValueAsString(snapshot.getGraph()));
            statement.setString(4, mapper.writeValueAsString(snapshot.getTaskDecision()));
            statement.setDate(5, new java.sql.Date(snapshot.getCreatedDate().getTime()));
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("sql error", e);
        } catch (JsonProcessingException e) {
            log.error("jackson error", e);
        }


    }
}
