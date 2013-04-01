package ru.taskurotta.server.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.config.ActorConfig;
import ru.taskurotta.server.config.ServerConfig;
import ru.taskurotta.server.config.expiration.ExpirationPolicy;

public class ExpiredTaskProcessorService implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ExpiredTaskProcessorService.class);
	
	private ServerConfig serverConfig;
	private TaskDao taskDao;
	private String schedule;
	private Map<String, ExpirationPolicy> expirationPolicyMap = new HashMap<String, ExpirationPolicy>();
	
	
	public void init() {
		if(serverConfig != null) {
			try {
				for(ActorConfig actorConfig: serverConfig.getActorConfigs()) {
					ExpirationPolicy expPolicy = null;
					ActorConfig.ExpirationPolicyConfig expPolicyConf = actorConfig.getExpirationPolicy();

					if(expPolicyConf!=null) {
						Class<?> expPolicyClass = Class.forName(expPolicyConf.getClassName());
						Properties expPolicyProps = expPolicyConf.getProperties();
						
						if(expPolicyProps != null) {
							expPolicy = (ExpirationPolicy) expPolicyClass.getConstructor(Properties.class).newInstance(expPolicyProps);	
						} else {
							expPolicy = (ExpirationPolicy) expPolicyClass.newInstance();
						}
						
						expirationPolicyMap.put(actorConfig.getActorQueueId(), expPolicy);
					}
					
				}
				
			} catch(Exception e) {
				logger.error("ExpiredTaskProcessorService#init invocation exception! ServerConfig["+serverConfig+"]", e);
				throw new RuntimeException(e);
			}
		}		
	}
	
	@Override
	public void run() {
		while(repeat(schedule)) {
			for(String actorQueueId: expirationPolicyMap.keySet()) {
				ExpirationPolicy ePolicy =  expirationPolicyMap.get(actorQueueId);
				taskDao.reScheduleTasks(actorQueueId, ePolicy);
			}
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
	
	
	private static boolean repeat(String schedule) {
		if(schedule == null) {
			return false;
		}
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
