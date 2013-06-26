package ru.taskurotta.backend.snapshot.datasource;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.snapshot.Snapshot;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import javax.sql.DataSource;
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

    private Gson gson;

    public JDBCSnapshotDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        gson = new Gson();
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
        snapshot.setSnapshotId((UUID) rs.getObject("snapshotId"));
        snapshot.setGraph(gson.fromJson(rs.getString("graph"), Graph.class));
        snapshot.setTask(gson.fromJson(rs.getString("task"), Task.class));
        snapshot.setTaskDecision(gson.fromJson(rs.getString("task_decision"), TaskDecision.class));
        snapshot.setCreatedDate(rs.getDate("created_date"));
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
            statement.setString(2, gson.toJson(snapshot.getTask()));
            statement.setString(3, gson.toJson(snapshot.getGraph()));
            statement.setString(4, gson.toJson(snapshot.getTaskDecision()));
            statement.setDate(5, new java.sql.Date(snapshot.getCreatedDate().getTime()));
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("sql error", e);
        }


    }
}
