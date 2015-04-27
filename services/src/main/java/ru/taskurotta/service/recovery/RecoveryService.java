package ru.taskurotta.service.recovery;

import java.util.Collection;
import java.util.UUID;

/**
 * Implements logic for task and processes recovery
 *
 * User: stukushin
 * Date: 21.10.13 18:24
 */
public interface RecoveryService {

    /**
     * @return true if any process's task have been placed to the queue
     */
    boolean recoverProcess(UUID processId);

    /**
     * @return UUID's collection of successfully restarted processes
     */
    Collection<UUID> recoverProcesses(Collection<UUID> processIds);

    /**
     * @return is task have been placed to queue
     */
    boolean recoverTask(UUID processId, UUID taskId);

}
