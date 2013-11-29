package ru.taskurotta.backend.gc;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 15.08.13
 * Time: 14:32
 */
public class GCTask implements Callable<Boolean> {

    private GeneralGarbageCollectorBackend generalGarbageCollectorBackend;
    private UUID processId;

    public GCTask(GeneralGarbageCollectorBackend generalGarbageCollectorBackend, UUID processId) {
        this.generalGarbageCollectorBackend = generalGarbageCollectorBackend;
        this.processId = processId;
    }

    @Override
    public Boolean call() throws Exception {
        generalGarbageCollectorBackend.delete(processId);
        return true;
    }
}
