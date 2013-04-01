package ru.taskurotta.server.config.expiration;

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.server.model.TaskObject;

/**
 *  Политика переотправки задания в очередь фиксированного(либо бесконечного) числа раз.
 */
public class FixedRetryPolicy implements ExpirationPolicy {
	
	private static final Logger logger = LoggerFactory.getLogger(FixedRetryPolicy.class);
	
	public static final String RETRY = "retry";
	public static final String TIMEOUT = "timeout";
	public static final String TIME_UNIT = "timeUnit";
	
	private Map<UUID, Integer> expirations = new ConcurrentHashMap<UUID, Integer>();
	
	private int retry = -1;
	private int timeout = -1;
	private TimeUnit timeUnit = TimeUnit.SECONDS;
	
	
	public FixedRetryPolicy(Properties props) {
		if(props!=null && !props.isEmpty()) {
			if(props.containsKey(RETRY)) {
				this.retry = Integer.valueOf(props.get(RETRY).toString());
			}

			if(props.containsKey(TIMEOUT)) {
				this.timeout = Integer.valueOf(props.get(TIMEOUT).toString());
			}

			if(props.containsKey(TIME_UNIT)) {
				this.timeUnit = TimeUnit.valueOf(props.get(TIME_UNIT).toString().toUpperCase());
			}
			
		}
		logger.debug("FixedRetryPolicy created. retry[{}], timeout[{}], timeUnit[{}], props[{}]", this.retry, this.timeout, this.timeUnit, props);
	}
	
	@Override
	public boolean isScheduleAgain(TaskObject task) {
		//TODO: some additional checks?
		return needRetry(task);
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
	public Date getNextExpirationDate(Date forDate) {
		Date result = null;
		if(timeout > 0) {
			long cur = forDate!=null? forDate.getTime(): 0;
			long expirationTime = timeUnit.toMillis(timeout);
			
			result = new Date(cur+expirationTime);
		}
		
		return result;
	}


}
