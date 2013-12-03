package ru.taskurotta.backend.gc;

import java.util.UUID;

public interface GarbageCollectorBackend {

    public void delete(UUID processId, String actorId);

}
