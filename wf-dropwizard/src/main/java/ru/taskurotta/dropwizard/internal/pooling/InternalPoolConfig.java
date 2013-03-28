package ru.taskurotta.dropwizard.internal.pooling;

import java.util.Properties;

public class InternalPoolConfig {
	public int poolSize = 10;
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
