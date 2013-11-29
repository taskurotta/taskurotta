package ru.taskurotta.backend.gc;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 15.08.13
 * Time: 14:32
 */
public class GCTask implements Callable<Boolean> {

    private GeneralGCBackend generalGCBackend;
    private UUID processId;

    public GCTask(GeneralGCBackend generalGCBackend, UUID processId) {
        this.generalGCBackend = generalGCBackend;
        this.processId = processId;
    }

    @Override
    public Boolean call() throws Exception {
        generalGCBackend.delete(processId);
        return true;
    }
}
