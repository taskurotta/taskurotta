package ru.taskurotta.oracle.test.runnable;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.dbcp.BasicDataSource;
import ru.taskurotta.oracle.test.DbDAO;
import ru.taskurotta.oracle.test.GenerationTools;
import ru.taskurotta.oracle.test.domain.SimpleTask;
import ru.taskurotta.oracle.test.domain.TaskStatus;

/**
 * User: greg
 */
public class CreationTask implements Runnable {

	private BasicDataSource dataSource;

	public CreationTask(BasicDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void run() {
		DbDAO dbDAO = new DbDAO(dataSource);
		int count = 0;
		try {
			System.out.println("Starting adding data");
			while (count < 1000000) {
				count++;
				dbDAO.enqueueTask(new SimpleTask(UUID.randomUUID(), GenerationTools.getRandomType(), new Date(), TaskStatus.CREATED, ""), "QUEUE_BUS");
			}
			System.out.println("Stopped adding data");
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}