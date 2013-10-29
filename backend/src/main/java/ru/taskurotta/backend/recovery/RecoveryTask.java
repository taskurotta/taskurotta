package ru.taskurotta.backend.recovery;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 15.08.13
 * Time: 14:32
 */
public class RecoveryTask implements Callable<Boolean> {

    private RecoveryProcessBackend recoveryProcessBackend;
    private UUID processId;

    public RecoveryTask(RecoveryProcessBackend recoveryProcessBackend, UUID processId) {
        this.recoveryProcessBackend = recoveryProcessBackend;
        this.processId = processId;
    }

    @Override
    public Boolean call() throws Exception {
        return recoveryProcessBackend.restartProcess(processId);

        //return null;
    }
}
