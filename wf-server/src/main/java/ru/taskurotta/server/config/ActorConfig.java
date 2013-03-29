package ru.taskurotta.server.config;

import java.util.concurrent.TimeUnit;


/**
 *  Configuration for every registered Actor, served by this TaskServer.
 */
public class ActorConfig {
	
	private String actorName;
	private long timeout = -1;
	private TimeUnit timeoutTimeUnit = TimeUnit.SECONDS;
	private String expirationPolicy;
	
	public String getExpirationPolicy() {
		return expirationPolicy;
	}
	public void setExpirationPolicy(String expirationPolicy) {
		this.expirationPolicy = expirationPolicy;
	}
	public String getActorName() {
		return actorName;
	}
	public void setActorName(String actorName) {
		this.actorName = actorName;
	}
	public long getTimeout() {
		return timeout;
	}
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	public TimeUnit getTimeoutTimeUnit() {
		return timeoutTimeUnit;
	}
	public void setTimeoutTimeUnit(TimeUnit timeoutTimeUnit) {
		this.timeoutTimeUnit = timeoutTimeUnit;
	}
	@Override
	public String toString() {
		return "ActorConfig [actorName=" + actorName + ", timeout=" + timeout
				+ ", timeoutTimeUnit=" + timeoutTimeUnit
				+ ", expirationPolicy=" + expirationPolicy + "]";
	}
		
}
