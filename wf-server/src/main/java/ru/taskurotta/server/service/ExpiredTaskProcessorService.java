package ru.taskurotta.server.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.config.expiration.ExpirationPolicy;
import ru.taskurotta.util.ActorDefinition;

public class ExpiredTaskProcessorService implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ExpiredTaskProcessorService.class);
	
	private ConfigBackend configBackend;
	private TaskDao taskDao;
	private String schedule;
	private final Map<ActorDefinition, ExpirationPolicy> expirationPolicyMap = new HashMap<ActorDefinition, ExpirationPolicy>();
	
	
	public void init() {
		if(configBackend != null) {
			try {
				for(ActorPreferences actorPrefs: configBackend.getActorPreferences()) {
					ExpirationPolicy expPolicy = null;
					ActorPreferences.ExpirationPolicyConfig expPolicyConf = actorPrefs.getExpirationPolicy();

					if(expPolicyConf!=null) {
						Class<?> expPolicyClass = Class.forName(expPolicyConf.getClassName());
						Properties expPolicyProps = expPolicyConf.getProperties();
						
						if(expPolicyProps != null) {
							expPolicy = (ExpirationPolicy) expPolicyClass.getConstructor(Properties.class).newInstance(expPolicyProps);	
						} else {
							expPolicy = (ExpirationPolicy) expPolicyClass.newInstance();
						}
						
						ActorDefinition actorDefinition = ActorDefinition.valueOf(actorPrefs.getClassName(), actorPrefs.getVersion()); 
						expirationPolicyMap.put(actorDefinition, expPolicy);
					}
					
				}
				
			} catch(Exception e) {
				logger.error("ExpiredTaskProcessorService#init invocation exception! configBackend["+configBackend+"]", e);
				throw new RuntimeException(e);
			}
		}		
	}
	
	@Override
	public void run() {
		while(repeat(schedule)) {
			for(ActorDefinition actorDef: expirationPolicyMap.keySet()) {
				ExpirationPolicy ePolicy =  expirationPolicyMap.get(actorDef);
				int reScheduled = taskDao.reScheduleTasks(actorDef, ePolicy);
				if(reScheduled > 0) {
					logger.info("[{}] tasks reScheduled due to expiration policy of actorId[{}]", reScheduled, actorDef);	
				}
			}
		}
	}

	public void setConfigBackend(ConfigBackend configBackend) {
		this.configBackend = configBackend;
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
