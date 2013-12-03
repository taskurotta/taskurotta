package ru.taskurotta.backend.gc;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 15.08.13
 * Time: 14:32
 */
public class GCTask implements Callable<Boolean> {

    private AbstractGCBackend abstractGCBackend;
    private UUID processId;

    public GCTask(AbstractGCBackend abstractGCBackend, UUID processId) {
        this.abstractGCBackend = abstractGCBackend;
        this.processId = processId;
    }

    @Override
    public Boolean call() throws Exception {
        abstractGCBackend.delete(processId);
        return true;
    }
}
