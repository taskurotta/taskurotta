package ru.taskurotta.service.config.model;

import java.util.UUID;

/**
 * Interface defining task expiration policy
 * I.e. defines when task is considered as expired
 */
public interface ExpirationPolicy {

    /**
     * @return long representation of server time when task consider to be expired
     */
    public long getExpirationTime(UUID taskId, UUID processId, long forTime);

    /**
     * @return is task ready to be recovered at it current state. For example, number of recovery attempts can be already exceeded
     */
    public boolean readyToRecover(UUID taskId, UUID processId);

}
