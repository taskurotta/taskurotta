package ru.taskurotta.oracle.test;

import org.apache.commons.dbcp.BasicDataSource;
import ru.taskurotta.oracle.test.domain.SimpleTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        final PreparedStatement ps = connection.prepareStatement("insert into queue_bus(job_id, user_id, type_job, data_update) values(?,?,?,?)");
        ps.setInt(1, task.getTaskId());
        ps.setInt(2, task.getUserId());
        ps.setInt(3, task.getTypeId());
        ps.setDate(4, new java.sql.Date(task.getDate().getTime()));
        ps.executeUpdate();
        ps.close();
        connection.close();
    }

    public boolean selectLastTaskWithTypeImproved(int typeJob) throws SQLException {
        final Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
		PreparedStatement ps = connection.prepareStatement(
				"select * from queue_bus where type_job=? and user_id=0 and rownum=1 order by data_update desc for update skip locked");
		ps.setInt(1, typeJob);
        ResultSet rs = ps.executeQuery();
        boolean updated = false;
        if (rs.next()) {
            updateJob(rs.getInt("job_id"), connection);
            updated = true;
        }
        connection.commit();
        ps.close();
		connection.setAutoCommit(true);
        connection.close();
        return updated;
    }

    private void updateJob(int job_id, Connection connection) throws SQLException {
        final PreparedStatement ps = connection.prepareStatement("update queue_bus set user_id=1 where job_id=?");
        ps.setInt(1, job_id);
        ps.executeUpdate();
        ps.close();
    }

    public void removeTask(int taskId, Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("delete from queue_bus where job_id=?");
        ps.setInt(1, taskId);
        ps.executeUpdate();
        ps.close();
        connection.close();
    }

}

