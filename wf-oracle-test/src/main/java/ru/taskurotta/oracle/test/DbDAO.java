package ru.taskurotta.oracle.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import ru.taskurotta.oracle.test.domain.SimpleTask;

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
		PreparedStatement ps = connection.prepareStatement(
				"UPDATE queue_bus q\n" +
						"SET q.user_id = 1\n" +
						"WHERE\n" +
						" q.job_id IN(\n" +
						"    SELECT\n" +
						"     job_id\n" +
						"    FROM\n" +
						"     queue_bus\n" +
						"    WHERE\n" +
						"     type_job = 0\n" +
						"    AND user_id = ?\n" +
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

