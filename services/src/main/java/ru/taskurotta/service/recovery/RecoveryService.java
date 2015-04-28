package ru.taskurotta.service.recovery;

import java.util.Collection;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 21.10.13
 * Time: 18:24
 */
public interface RecoveryService {
    /**
     * @return result of restart process
     */
    boolean resurrectProcess(UUID processId);

    /**
     * @return UUID's collection of successfully restarted processes
     */
    Collection<UUID> resurrectProcesses(Collection<UUID> processIds);

    /**
     * @return result for aborting process
     */
    boolean abortProcess(UUID processId);

    /**
     * @param processId
     * @param taskId
     * @return true if task has been successfully restrated and placed to actor's queue
     */
    boolean restartTask(UUID processId, UUID taskId);

}
