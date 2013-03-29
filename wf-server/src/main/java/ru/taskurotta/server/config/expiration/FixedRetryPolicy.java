package ru.taskurotta.server.config.expiration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ru.taskurotta.server.model.TaskObject;

/**
 *  Политика переотправки задания в очередь фиксированного(либо бесконечного) числа раз.
 */
public class FixedRetryPolicy implements ExpirationPolicy {
	
	private Map<UUID, Integer> expirations = new ConcurrentHashMap<UUID, Integer>();
	
	private int retry = -1;
	
	private String policyName;
	
	@Override
	public boolean isScheduleAgain(TaskObject task) {
		boolean result = true;
		if(retry > 0) {
			Integer taskRetry = expirations.get(task.getTaskId());
			
			if(taskRetry == null || taskRetry < retry) {
				expirations.put(task.getTaskId(), Integer.valueOf(taskRetry==null?1: taskRetry.intValue()+1));
			} else {
				result = false;
			}
			
		}
		return result;
	}

	@Override
	public String getPolicyName() {
		return policyName;
	}

	public void setRetry(int retry) {
		this.retry = retry;
	}

	public void setPolicyName(String policyName) {
		this.policyName = policyName;
	}


}
