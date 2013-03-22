package ru.taskurotta.oracle.test;

import org.apache.commons.dbcp.BasicDataSource;
import ru.taskurotta.oracle.test.domain.SimpleTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * User: greg
 */

public class DbDAO {
    private BasicDataSource dataSource;

    public DbDAO(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insertData(SimpleTask task) throws SQLException {
        final Connection connection = dataSource.getConnection();
        final PreparedStatement ps = connection.prepareStatement("insert into queue_bus(task_id, status_id, type_id, data_update, actor_id) values(?,?,?,?,?)");
        ps.setInt(1, task.getTaskId());
        ps.setInt(2, task.getStatusId());
        ps.setInt(3, task.getTypeId());
        ps.setDate(4, new java.sql.Date(task.getDate().getTime()));
        ps.setString(5, task.getActorId());
        ps.executeUpdate();
        ps.close();
        connection.close();
    }

    public boolean selectLastTaskWithTypeImproved(int typeJob) throws SQLException {
        final Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
                "UPDATE queue_bus q\n" +
                        "SET q.status_id = 1\n" +
                        "WHERE\n" +
                        " q.task_id IN(\n" +
                        "    SELECT\n" +
                        "     task_id\n" +
                        "    FROM\n" +
                        "     queue_bus\n" +
                        "    WHERE\n" +
                        "     type_id = 0\n" +
                        "    AND status_id = ?\n" +
                        "    AND\n" +
                        "     data_update <= CURRENT_TIMESTAMP\n" +
                        "   AND\n" +
                        "   ROWNUM = 1\n" +
                        " )");
        ps.setInt(1, typeJob);
        ps.executeUpdate();
        ps.close();
        connection.commit();
        connection.close();
        return true;
    }

}

