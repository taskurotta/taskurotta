package ru.taskurotta.server.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.config.ServerConfig;

public class ExpiredTaskProcessorService implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ExpiredTaskProcessorService.class);
	
	private ServerConfig serverConfig;
	private TaskDao taskDao;
	private String schedule;

	@Override
	public void run() {
		while(shouldRepeat(schedule)) {
			System.out.println("console: Scheduled check for expired task...");
			logger.info("log: Scheduled check for expired task...");					
		}
	}

	public void setServerConfig(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}

	public void setTaskDao(TaskDao taskDao) {
		this.taskDao = taskDao;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
	
	private static boolean shouldRepeat(String schedule) {
		Integer number = Integer.valueOf(schedule.replaceAll("\\D", "").trim());
		TimeUnit unit = TimeUnit.valueOf(schedule.replaceAll("\\d", "").trim());
		try {
			Thread.sleep(unit.toMillis(number));
		} catch (InterruptedException e) {
			logger.error("ExpiredTaskProcessorService schedule interrupted", e);
		}
		return true;
	}
	
	
}
