package ru.taskurotta.service.recovery;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 15.08.13
 * Time: 14:32
 */
public class RecoveryTask implements Callable<Boolean> {

    private RecoveryProcessService recoveryProcessService;
    private UUID processId;

    public RecoveryTask(RecoveryProcessService recoveryProcessService, UUID processId) {
        this.recoveryProcessService = recoveryProcessService;
        this.processId = processId;
    }

    @Override
    public Boolean call() throws Exception {
        return recoveryProcessService.restartProcess(processId);
    }
}
