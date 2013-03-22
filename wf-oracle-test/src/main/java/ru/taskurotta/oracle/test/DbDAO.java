package ru.taskurotta.oracle.test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

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
		CallableStatement cs = connection.prepareCall(
				"begin\n" +
						"UPDATE queue_bus q\n" +
						" SET q.STATUS_ID = 1\n" +
						" WHERE\n" +
						"  q.TASK_ID IN(\n" +
						"   SELECT\n" +
						"    TASK_ID\n" +
						"   FROM\n" +
						"    queue_bus\n" +
						"   WHERE\n" +
						"    TYPE_ID = ?\n" +
						"   AND STATUS_ID = 0\n" +
						"   AND\n" +
						"    data_update <= CURRENT_TIMESTAMP\n" +
						"   AND\n" +
						"   ROWNUM = 1\n" +
						"  )\n" +
						"RETURNING TASK_ID INTO ?;END;");
		cs.setInt(1, typeJob);
		cs.registerOutParameter(2, Types.BIGINT);

		cs.execute();
		Long job_id = cs.getLong(2);
		cs.close();
		connection.close();
		return (job_id != null) && (job_id > 0);
	}

}

