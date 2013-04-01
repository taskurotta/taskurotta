package ru.taskurotta.oracle.test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.oracle.test.domain.SimpleTask;

/**
 * User: greg
 */

public class DbDAO {

	private final static Logger log = LoggerFactory.getLogger(DbDAO.class);

	private DataSource dataSource;

	public DbDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void enqueueTask(SimpleTask task, String queueName) throws SQLException {
		final Connection connection = dataSource.getConnection();
		final PreparedStatement ps = connection.prepareStatement("insert into " + queueName + " (task_id, status_id, type_id, data_update, actor_id) values (?,?,?,?,?)");
		ps.setString(1, task.getTaskId().toString());
		ps.setInt(2, task.getStatusId());
		ps.setInt(3, task.getTypeId());
		ps.setDate(4, new java.sql.Date(task.getDate().getTime()));
		ps.setString(5, task.getActorId());
		ps.executeUpdate();
		ps.close();
		connection.close();
	}

	public void dequeueTask(int taskId, String queueName) throws SQLException {
		final Connection connection = dataSource.getConnection();
		final PreparedStatement ps = connection.prepareStatement("delete from " + queueName + " where task_id = ?");
		ps.setInt(1, taskId);
		ps.executeUpdate();
		ps.close();
		connection.close();
	}

	public boolean queueExists(String queueName) throws SQLException {
		boolean result;
		final Connection connection = dataSource.getConnection();
		String query = "SELECT COUNT(*) cnt FROM dba_tables where table_name = ?";
		final PreparedStatement ps = connection.prepareStatement(query);
		ps.setString(1, queueName.toUpperCase());
		ResultSet rs = ps.executeQuery();
		int count = 0;
		if (rs.next()) {
			count = rs.getInt("cnt");
		}
		result = count > 0;
		ps.close();
		connection.close();
		return result;
	}

	public void createQueue(String queueName) throws SQLException {
		log.warn("!!!!! Creating queue = " + queueName);
		final Connection connection = dataSource.getConnection();
		connection.setAutoCommit(false);

		String createQuery = "CREATE TABLE :queue_name \n" +
				"   (\n" +
				" TASK_ID VARCHAR(36) NOT NULL ENABLE, \n" +
				" STATUS_ID NUMBER NOT NULL ENABLE, \n" +
				" TYPE_ID NUMBER NOT NULL ENABLE, \n" +
				" DATA_UPDATE DATE, \n" +
				" ACTOR_ID VARCHAR2(100), \n" +
				" PRIMARY KEY (TASK_ID))";
		String indexQuery = "CREATE INDEX :queue_name_IND ON :queue_name (STATUS_ID, TYPE_ID)";
		Statement statement = connection.createStatement();
		statement.addBatch(createQuery.replace(":queue_name", queueName));
		statement.addBatch(indexQuery.replace(":queue_name", queueName));
		statement.executeBatch();
		connection.commit();
		statement.close();
		connection.setAutoCommit(true);
		connection.close();
	}

	public UUID pullTask(String queueName) throws SQLException {
		final Connection connection = dataSource.getConnection();
		String query = "begin\n" +
				"UPDATE %s\n" +
				"SET STATUS_ID = 1\n" +
				"WHERE\n" +
				"STATUS_ID = 0\n" +
				"AND data_update <= CURRENT_TIMESTAMP\n" +
				"AND ROWNUM = 1\n" +
				"RETURNING TASK_ID INTO ?;END;";
		CallableStatement cs = connection.prepareCall(String.format(query, queueName));
		cs.registerOutParameter(1, Types.VARCHAR);
		cs.execute();
		UUID job_id = UUID.fromString(cs.getString(1));
		cs.close();
		connection.close();
		return job_id;
	}

	public Map<String, Long> getQueueNames() throws SQLException {
		Map<String, Long> result = new HashMap<String, Long>();
		final Connection connection = dataSource.getConnection();
		String query = "SELECT * FROM QB$QUEUE_NAMES";
		final PreparedStatement ps = connection.prepareStatement(query);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			result.put(rs.getString("queue_name"), rs.getLong("queue_id"));
		}
		ps.close();
		connection.close();
		return result;
	}

	public long registerQueue(String queueName) throws SQLException {
		long result = -1;
		final Connection connection = dataSource.getConnection();
		String query = "begin\n INSERT INTO QB$QUEUE_NAMES (QUEUE_ID, QUEUE_NAME) VALUES (QB$SEQUENCE.nextval,?) RETURNING QUEUE_ID INTO ?;END;";
		final CallableStatement ps = connection.prepareCall(query);
		ps.setString(1, queueName);
		ps.registerOutParameter(2, Types.BIGINT);
		ps.execute();
		result = ps.getLong(2);
		ps.close();
		connection.close();
		return result;
	}

}

