package ru.taskurotta.backend.config.model;

import java.util.UUID;

/**
 * Interface defining task expiration policy
 * I.e. defines when task is considered as expired
 */
public interface ExpirationPolicy {

    public long getExpirationTime(UUID taskUuid, long forTime);

    public boolean readyToRecover(UUID uuid);

}
