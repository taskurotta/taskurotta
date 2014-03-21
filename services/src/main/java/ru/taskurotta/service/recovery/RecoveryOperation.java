package ru.taskurotta.service.recovery;

import ru.taskurotta.service.executor.Operation;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 24.12.13
 * Time: 12:29
 */
public class RecoveryOperation implements Operation {

    private UUID processId;

    private RecoveryProcessService recoveryProcessService;

    public RecoveryOperation(UUID processId) {
        this.processId = processId;
    }

    @Override
    public void init(Object nativePoint) {
        this.recoveryProcessService = (RecoveryProcessService) nativePoint;
    }

    @Override
    public void run() {
        recoveryProcessService.restartProcess(processId);
    }
}
