package ru.taskurotta.backend.recovery;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 21.10.13
 * Time: 18:24
 */
public interface RecoveryProcessBackend {
    void restartProcess(UUID processId);
}
