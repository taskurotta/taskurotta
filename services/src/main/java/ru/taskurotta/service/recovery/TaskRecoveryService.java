package ru.taskurotta.service.recovery;

import java.util.UUID;

/**
 * Created on 24.04.2015.
 */
public interface TaskRecoveryService {

    boolean recover(UUID processId, UUID taskId);

}
