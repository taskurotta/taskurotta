package ru.taskurotta.server.config.expiration;

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import ru.taskurotta.server.model.TaskObject;

/**
 *  Политика переотправки задания в очередь фиксированного(либо бесконечного) числа раз.
 */
public class FixedRetryPolicy implements ExpirationPolicy {
	
	public static final String RETRY = "retry";
	public static final String TIMEOUT = "timeout";
	public static final String TIME_UNIT = "timeUnit";
	
	private Map<UUID, Integer> expirations = new ConcurrentHashMap<UUID, Integer>();
	
	private int retry = -1;
	private int timeout = -1;
	private TimeUnit timeUnit = TimeUnit.SECONDS;
	
	
	public FixedRetryPolicy(Properties props) {
		if(props!=null && props.isEmpty()) {
			if(props.contains(RETRY)) {
				retry = Integer.valueOf(props.getProperty(RETRY));
			}

			if(props.contains(TIMEOUT)) {
				timeout = Integer.valueOf(props.getProperty(TIMEOUT));
			}

			if(props.contains(TIME_UNIT)) {
				timeUnit = TimeUnit.valueOf(props.getProperty(TIME_UNIT).toUpperCase());
			}
			
		}
		
	}
	
	@Override
	public boolean isScheduleAgain(TaskObject task) {
		return false;
	}

	
	private boolean needRetry(TaskObject task) {
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
	public Date getNextExpirationDate() {
		Date result = null;
		if(timeout > 0) {
			long cur = System.currentTimeMillis();
			long expirationTime = timeUnit.toMillis(timeout);
			
			result = new Date(cur+expirationTime);			
		}
		
		return result;
	}


}
