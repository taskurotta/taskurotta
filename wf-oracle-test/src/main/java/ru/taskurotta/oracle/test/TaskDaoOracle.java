package ru.taskurotta.oracle.test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.oracle.test.domain.SimpleTask;
import ru.taskurotta.server.memory.TaskDaoMemory;
import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: greg
 */
public class TaskDaoOracle extends TaskDaoMemory {

	private final static Logger log = LoggerFactory.getLogger(TaskDaoOracle.class);

	private final DbDAO dbDAO;
	private final HashMap<String, Long> queueNames = new HashMap<String, Long>();

	public TaskDaoOracle(DbConnect dbConnect) {
		dbDAO = new DbDAO(dbConnect.getDataSource());
		try {
			queueNames.putAll(dbDAO.getQueueNames());
		} catch (SQLException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	private void init() {

	}

	@Override
	protected void addTaskToQueue(TaskObject taskObj) {
		//super.add(taskObj);
		try {
			String queueName = getQueueName(taskObj.getTarget().getName(), taskObj.getTarget().getVersion());

			if (!queueNames.containsKey(queueName)) {
				long queueId = dbDAO.registerQueue(queueName);
				queueNames.put(queueName, queueId);
				dbDAO.createQueue("qb$" + queueId);

			}

			dbDAO.enqueueTask(SimpleTask.createFromTaskObject(taskObj), getTableName(queueName));
		} catch (SQLException ex) {
			log.error("Database error", ex);
		}
	}

	@Override
	public TaskObject pull(ActorDefinition actorDefinition) {
		String queueName = getTableName(getQueueName(actorDefinition.getName(), actorDefinition.getVersion()));

		try {
			final UUID taskId = dbDAO.pullTask(queueName);
			return taskMap.get(taskId);
		} catch (SQLException ex) {
			log.error("Database error", ex);
			return null;
		}

	}

	private String getTableName(String queueName) {
		Long id = queueNames.get(queueName);
		if (id != null) {
			return "qb$" + id;
		}
		return null;
	}


}
