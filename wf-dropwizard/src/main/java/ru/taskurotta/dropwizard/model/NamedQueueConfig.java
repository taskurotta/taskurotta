package ru.taskurotta.dropwizard.model;

public class NamedQueueConfig {
	
	public static final String CACHED_POOL = "cached";
	public static final String FIXED_POOL = "fixed";
	public static final String SCHEDULED_POOL = "scheduled";
	public static final String SINGLE_POOL = "single";
	
	private String consumerClass = "any";
	private String poolType = FIXED_POOL;
	private int minThreads = 1;
	private int maxThreads = 10;

	
	public String getConsumerClass() {
		return consumerClass;
	}
	public void setConsumerClass(String consumerClass) {
		this.consumerClass = consumerClass;
	}
	public String getPoolType() {
		return poolType;
	}
	public void setPoolType(String poolType) {
		this.poolType = poolType;
	}
	public int getMinThreads() {
		return minThreads;
	}
	public void setMinThreads(int minThreads) {
		this.minThreads = minThreads;
	}
	public int getMaxThreads() {
		return maxThreads;
	}
	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}
	
}
