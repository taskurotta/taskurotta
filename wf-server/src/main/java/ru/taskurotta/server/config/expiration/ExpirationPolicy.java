package ru.taskurotta.server.config.expiration;

import java.util.UUID;

/**
 * Interface defining task expiration recovery policy
 *
 */
public interface ExpirationPolicy {
	
	public long getExpirationTimeout(long forTime);
	
	public long getNextStartTime(UUID taskUuid, long taskStartTime);
	
	public boolean readyToRecover(UUID uuid);
	
}
