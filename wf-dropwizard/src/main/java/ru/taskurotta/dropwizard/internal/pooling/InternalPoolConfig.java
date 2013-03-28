package ru.taskurotta.dropwizard.internal.pooling;

import java.util.Properties;

/**
 * Internal pool feature config
 */
public class InternalPoolConfig {
	/**
	 * Size of the fixed thread pool
	 */
	public int poolSize = 10;
	
	/**
	 * Timeout for thread to wait for pooled task to complete
	 */
	public long threadTimeout = 5000;
	
	public int getPoolSize() {
		return poolSize;
	}
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}
	public long getThreadTimeout() {
		return threadTimeout;
	}
	public void setThreadTimeout(long threadTimeout) {
		this.threadTimeout = threadTimeout;
	}
	
	//Properties exposed to spring application context
	public Properties asProperties() {
		Properties result = new Properties();
		
		result.put("internalPool.poolSize", poolSize);
		result.put("internalPool.threadTimeout", threadTimeout);
		
		return result;
	}
	
	@Override
	public String toString() {
		return "InternalPoolConfig [poolSize=" + poolSize + ", threadTimeout="
				+ threadTimeout + "]";
	}
	
	
}
