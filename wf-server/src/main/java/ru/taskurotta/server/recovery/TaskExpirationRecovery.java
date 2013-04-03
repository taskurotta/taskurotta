package ru.taskurotta.server.recovery;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.ConfigBackendAware;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.StorageBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.server.config.expiration.ExpirationPolicy;
import ru.taskurotta.util.ActorDefinition;

public class TaskExpirationRecovery implements Runnable, ConfigBackendAware {

	private static final Logger logger = LoggerFactory.getLogger(TaskExpirationRecovery.class);

	private QueueBackend queueBackend;
	private StorageBackend storageBackend;

	private String schedule;
	private int limit;

	private Map<ActorDefinition, ExpirationPolicy> expirationPolicyMap;

	private String lockingGuid;


	public TaskExpirationRecovery() {
		lockingGuid = "TaskExpirationRecovery#" + UUID.randomUUID().toString();
	}

	@Override
	public void run() {
		logger.debug("TaskExpirationRecovery daemon started. Schedule[{}], limit[{}], expirationPolicies for[{}]", schedule, limit, expirationPolicyMap!=null? expirationPolicyMap.keySet(): null);
		while(repeat(schedule)) {
			if(expirationPolicyMap!=null && !expirationPolicyMap.isEmpty()) {
				for(ActorDefinition actorDef: expirationPolicyMap.keySet()) {
					ExpirationPolicy ePolicy =  expirationPolicyMap.get(actorDef);
					long timeout = ePolicy.getExpirationTimeout(new Date());

					List<TaskContainer> expiredTasks = storageBackend.getExpiredTasks(actorDef, timeout, limit);
					if(expiredTasks!=null && !expiredTasks.isEmpty()) {
						logger.debug("Try to recover [{}] tasks", expiredTasks.size());
						int counter = 0;
						for(TaskContainer task: expiredTasks) {
							if(ePolicy.readyToRecover(task.getTaskId())) {
								try {
									storageBackend.lockTask(task.getTaskId(), lockingGuid, new Date(new Date().getTime()+timeout));
									TaskTarget taskTarget = task.getTarget();
									queueBackend.enqueueItem(ActorDefinition.valueOf(taskTarget.getName(), taskTarget.getVersion()), task.getTaskId(), ePolicy.getNextStartTime(task.getTaskId(), task.getStartTime()));
									counter++;
								} catch(Exception e) {
									logger.error("Cannot recover task["+task.getTaskId()+"]", e);
								} finally {
									storageBackend.unlockTask(task.getTaskId(), lockingGuid);
									//TODO: is all this locking/unlocking really required?
								}

							} else {
								logger.error("Cannot perform expired task recovery. Task[{}]", task);
								//TODO: execute error processing in backends
							}

						}
						
						logger.info("Recovered [{}]/[{}] tasks due to expiration policy of actor[{}]", counter, expiredTasks.size(), actorDef);	
						
					}
				}				
			}
		}
	}

	private void initConfigs(ActorPreferences[] actorPrefs) {
		logger.debug("Initializing recovery config with actorPrefs[{}]", actorPrefs);
		if(actorPrefs!=null) {
			try {
				expirationPolicyMap = new HashMap<ActorDefinition, ExpirationPolicy>();
				for(ActorPreferences actorConfig: actorPrefs) {
					ExpirationPolicy expPolicy = null;
					ActorPreferences.ExpirationPolicyConfig expPolicyConf = actorConfig.getExpirationPolicy();

					if(expPolicyConf!=null) {
						Class<?> expPolicyClass = Class.forName(expPolicyConf.getClassName());
						Properties expPolicyProps = expPolicyConf.getProperties();

						if(expPolicyProps != null) {
							expPolicy = (ExpirationPolicy) expPolicyClass.getConstructor(Properties.class).newInstance(expPolicyProps);	
						} else {
							expPolicy = (ExpirationPolicy) expPolicyClass.newInstance();
						}

						expirationPolicyMap.put(ActorDefinition.valueOf(actorConfig.getClassName(), actorConfig.getVersion()), expPolicy);
					}

				}

			} catch(Exception e) {
				logger.error("TaskExpirationRecovery#initConfigs invocation failed! actorPrefs["+actorPrefs+"]", e);
				throw new RuntimeException(e);
			}			
		}
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

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void setQueueBackend(QueueBackend queueBackend) {
		this.queueBackend = queueBackend;
	}

	public void setStorageBackend(StorageBackend storageBackend) {
		this.storageBackend = storageBackend;
	}

	public void setConfigBackend(ConfigBackend configBackend) {
		initConfigs(configBackend.getActorPreferences());
	}

}
