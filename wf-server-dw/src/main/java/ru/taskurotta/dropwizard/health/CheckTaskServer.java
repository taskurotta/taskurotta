package ru.taskurotta.dropwizard.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.util.ActorDefinition;

import com.yammer.metrics.core.HealthCheck;

public class CheckTaskServer extends HealthCheck {

	private static final Logger logger = LoggerFactory.getLogger(CheckTaskServer.class);
	
	private TaskServer taskServer;
	
	public CheckTaskServer(String name) {
		super(name);
	}

	@Override
	protected Result check() throws Exception {
		try {
			taskServer.pull( ActorDefinition.valueOf("testme", "testme"));
			return Result.healthy();	
		} catch(Exception e) {
			logger.error("CheckTaskServer failed!", e);
			return Result.unhealthy(e);
		}
		
	}

	@Required
	public void setTaskServer(TaskServer taskServer) {
		this.taskServer = taskServer;
	}

}
